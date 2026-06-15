package com.loyaltyos.access.service;

import com.loyaltyos.access.dto.InviteUserRequest;
import com.loyaltyos.access.dto.ReassignUserRoleRequest;
import com.loyaltyos.access.dto.TenantUserResponse;
import com.loyaltyos.access.dto.UpdateUserRequest;
import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.entity.TenantUserRole;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.enums.TenantUserStatus;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.repository.TenantUserRoleRepository;
import com.loyaltyos.access.security.TenantPasswordPolicy;
import com.loyaltyos.access.security.TenantTempPasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TenantUserAdminService {

    private static final Logger log = LoggerFactory.getLogger(TenantUserAdminService.class);

    private final TenantUserRepository userRepository;
    private final TenantUserRoleRepository userRoleRepository;
    private final TenantRoleRepository roleRepository;
    private final PrivilegeGrantRepository grantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessProvisioningService provisioningService;
    private final TenantInviteTokenService inviteTokenService;
    private final TenantInviteMailer inviteMailer;
    private final AccessControlAuditService auditService;

    public TenantUserAdminService(
        TenantUserRepository userRepository,
        TenantUserRoleRepository userRoleRepository,
        TenantRoleRepository roleRepository,
        PrivilegeGrantRepository grantRepository,
        PasswordEncoder passwordEncoder,
        AccessProvisioningService provisioningService,
        TenantInviteTokenService inviteTokenService,
        TenantInviteMailer inviteMailer,
        AccessControlAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.grantRepository = grantRepository;
        this.passwordEncoder = passwordEncoder;
        this.provisioningService = provisioningService;
        this.inviteTokenService = inviteTokenService;
        this.inviteMailer = inviteMailer;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TenantUserResponse> listUsers(String tenantId) {
        return userRepository.findByTenantIdOrderByEmailAsc(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public TenantUserResponse inviteUser(String tenantId, InviteUserRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        var role = roleRepository.findById(request.getRoleId())
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        boolean manualTempPassword = request.getTemporaryPassword() != null
            && !request.getTemporaryPassword().isBlank();
        String rawTempPassword = manualTempPassword
            ? request.getTemporaryPassword().trim()
            : TenantTempPasswordGenerator.generate();
        if (manualTempPassword) {
            TenantPasswordPolicy.validateNewPassword(rawTempPassword, email, null);
        }

        TenantUser user = new TenantUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(rawTempPassword));
        user.setStatus(TenantUserStatus.ACTIVE);
        user.setMustChangePassword(true);
        user.setSessionVersion(1);
        userRepository.save(user);
        userRoleRepository.save(new TenantUserRole(user.getUserId(), role.getRoleId(), tenantId));

        TenantUserResponse response = toResponse(user);
        boolean emailSent = false;
        if (manualTempPassword) {
            log.info("Team invite for {} using MANUAL_TEMP_PASSWORD path (no email)", email);
        } else {
            log.info("Team invite for {} using EMAIL_TEMP_PASSWORD path", email);
            emailSent = inviteMailer.sendTempPasswordInviteEmail(email, rawTempPassword);
        }
        response.setInviteEmailSent(emailSent);

        auditService.logFromSecurityContext(tenantId, "USER_INVITED",
            Map.of(
                "userId", user.getUserId(),
                "email", email,
                "roleId", role.getRoleId(),
                "inviteMode", manualTempPassword ? "MANUAL_TEMP_PASSWORD" : "EMAIL_TEMP_PASSWORD",
                "inviteEmailSent", emailSent
            ));
        return response;
    }

    @Transactional
    public void acceptInvite(String email, String token, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        TenantInviteTokenService.InviteTarget target = inviteTokenService.consumeToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invite token"));

        TenantUser user = userRepository.findById(target.userId())
            .filter(u -> target.tenantId().equals(u.getTenantId()))
            .filter(u -> normalizedEmail.equalsIgnoreCase(u.getEmail()))
            .orElseThrow(() -> new IllegalArgumentException("Invite does not match this user"));

        if (user.getStatus() != TenantUserStatus.INVITED) {
            throw new IllegalStateException("Invite has already been accepted");
        }

        TenantPasswordPolicy.validateNewPassword(rawPassword, normalizedEmail, null);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(TenantUserStatus.ACTIVE);
        user.setMustChangePassword(false);
        userRepository.save(user);
        auditService.log(target.tenantId(), "TENANT_USER", user.getUserId(), "USER_INVITE_ACCEPTED",
            Map.of("email", normalizedEmail));
    }

    @Transactional
    public TenantUserResponse reassignUserRole(String tenantId, String userId, ReassignUserRoleRequest request) {
        TenantUser user = userRepository.findById(userId)
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() == TenantUserStatus.DISABLED) {
            throw new IllegalStateException("Cannot reassign a disabled user");
        }
        if (isPrimaryAdminUser(tenantId, userId)) {
            throw new IllegalArgumentException("Cannot reassign Primary Administrator");
        }

        var newRole = roleRepository.findById(request.getRoleId())
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        if (newRole.isSystem()) {
            throw new IllegalArgumentException("Cannot assign a system role through reassignment");
        }

        String previousRoleId = userRoleRepository.findByUserId(userId).stream()
            .map(TenantUserRole::getRoleId)
            .findFirst()
            .orElse(null);
        if (request.getRoleId().equals(previousRoleId)) {
            return toResponse(user);
        }

        userRoleRepository.deleteByUserId(userId);
        userRoleRepository.save(new TenantUserRole(userId, newRole.getRoleId(), tenantId));
        grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(tenantId, GrantSubjectType.USER, userId);
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);

        auditService.logFromSecurityContext(tenantId, "USER_ROLE_REASSIGNED",
            Map.of(
                "userId", userId,
                "email", user.getEmail(),
                "previousRoleId", previousRoleId != null ? previousRoleId : "",
                "newRoleId", newRole.getRoleId(),
                "newRoleName", newRole.getRoleName()
            ));
        return toResponse(user);
    }

    @Transactional
    public TenantUserResponse updateUser(String tenantId, String userId, UpdateUserRequest request) {
        TenantUser user = userRepository.findById(userId)
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() == TenantUserStatus.DISABLED) {
            throw new IllegalStateException("Cannot edit a disabled user");
        }

        String trimmedName = request.getFullName() != null ? request.getFullName().trim() : null;
        String normalizedName = trimmedName != null && !trimmedName.isBlank() ? trimmedName : null;
        boolean nameChanged = !Objects.equals(normalizedName, user.getFullName());
        if (nameChanged) {
            user.setFullName(normalizedName);
        }

        String previousRoleId = userRoleRepository.findByUserId(userId).stream()
            .map(TenantUserRole::getRoleId)
            .findFirst()
            .orElse(null);
        boolean roleChanged = !request.getRoleId().equals(previousRoleId);
        if (roleChanged) {
            if (isPrimaryAdminUser(tenantId, userId)) {
                throw new IllegalArgumentException("Cannot reassign Primary Administrator");
            }
            var newRole = roleRepository.findById(request.getRoleId())
                .filter(r -> tenantId.equals(r.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            if (newRole.isSystem()) {
                throw new IllegalArgumentException("Cannot assign a system role");
            }
            userRoleRepository.deleteByUserId(userId);
            userRoleRepository.save(new TenantUserRole(userId, newRole.getRoleId(), tenantId));
            grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(tenantId, GrantSubjectType.USER, userId);
            user.setSessionVersion(user.getSessionVersion() + 1);
        }

        if (nameChanged || roleChanged) {
            userRepository.save(user);
            auditService.logFromSecurityContext(tenantId, "USER_UPDATED",
                Map.of(
                    "userId", userId,
                    "email", user.getEmail(),
                    "fullNameChanged", nameChanged,
                    "roleChanged", roleChanged,
                    "previousRoleId", previousRoleId != null ? previousRoleId : "",
                    "newRoleId", request.getRoleId()
                ));
        }
        return toResponse(user);
    }

    @Transactional
    public void disableUser(String tenantId, String userId) {
        TenantUser user = userRepository.findById(userId)
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (isPrimaryAdminUser(tenantId, userId)) {
            throw new IllegalArgumentException("Cannot disable Primary Administrator");
        }
        user.setStatus(TenantUserStatus.DISABLED);
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
        auditService.logFromSecurityContext(tenantId, "USER_DISABLED", Map.of("userId", userId));
    }

    private boolean isPrimaryAdminUser(String tenantId, String userId) {
        return roleRepository.findByTenantIdAndSystemTrue(tenantId)
            .map(r -> userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> ur.getRoleId().equals(r.getRoleId())))
            .orElse(false);
    }

    private TenantUserResponse toResponse(TenantUser user) {
        TenantUserResponse r = new TenantUserResponse();
        r.setUserId(user.getUserId());
        r.setEmail(user.getEmail());
        r.setFullName(user.getFullName());
        r.setStatus(user.getStatus().name());
        r.setMustChangePassword(user.isMustChangePassword());
        r.setRoleIds(userRoleRepository.findByUserId(user.getUserId()).stream().map(TenantUserRole::getRoleId).toList());
        return r;
    }
}
