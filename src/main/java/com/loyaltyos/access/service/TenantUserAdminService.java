package com.loyaltyos.access.service;

import com.loyaltyos.access.dto.InviteUserRequest;
import com.loyaltyos.access.dto.TenantUserResponse;
import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.entity.TenantUserRole;
import com.loyaltyos.access.enums.TenantUserStatus;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.repository.TenantUserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantUserAdminService {

    private final TenantUserRepository userRepository;
    private final TenantUserRoleRepository userRoleRepository;
    private final TenantRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessProvisioningService provisioningService;
    private final TenantInviteTokenService inviteTokenService;
    private final TenantInviteMailer inviteMailer;
    private final AccessControlAuditService auditService;

    public TenantUserAdminService(
        TenantUserRepository userRepository,
        TenantUserRoleRepository userRoleRepository,
        TenantRoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AccessProvisioningService provisioningService,
        TenantInviteTokenService inviteTokenService,
        TenantInviteMailer inviteMailer,
        AccessControlAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
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

        TenantUser user = new TenantUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setStatus(TenantUserStatus.INVITED);
        user.setSessionVersion(1);
        if (request.getTemporaryPassword() != null && !request.getTemporaryPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getTemporaryPassword()));
            user.setStatus(TenantUserStatus.ACTIVE);
        }
        userRepository.save(user);
        userRoleRepository.save(new TenantUserRole(user.getUserId(), role.getRoleId(), tenantId));

        TenantUserResponse response = toResponse(user);
        if (user.getStatus() == TenantUserStatus.INVITED) {
            String token = inviteTokenService.issueToken(tenantId, user.getUserId());
            response.setInviteToken(token);
            inviteMailer.sendInviteEmail(email, token);
        }
        auditService.logFromSecurityContext(tenantId, "USER_INVITED",
            Map.of("userId", user.getUserId(), "email", email, "roleId", role.getRoleId()));
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

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(TenantUserStatus.ACTIVE);
        userRepository.save(user);
        auditService.log(target.tenantId(), "TENANT_USER", user.getUserId(), "USER_INVITE_ACCEPTED",
            Map.of("email", normalizedEmail));
    }

    @Transactional
    public void disableUser(String tenantId, String userId) {
        TenantUser user = userRepository.findById(userId)
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (roleRepository.findByTenantIdAndSystemTrue(tenantId)
            .map(r -> userRoleRepository.findByUserId(userId).stream().anyMatch(ur -> ur.getRoleId().equals(r.getRoleId())))
            .orElse(false)) {
            throw new IllegalArgumentException("Cannot disable Primary Administrator");
        }
        user.setStatus(TenantUserStatus.DISABLED);
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
        auditService.logFromSecurityContext(tenantId, "USER_DISABLED", Map.of("userId", userId));
    }

    private TenantUserResponse toResponse(TenantUser user) {
        TenantUserResponse r = new TenantUserResponse();
        r.setUserId(user.getUserId());
        r.setEmail(user.getEmail());
        r.setFullName(user.getFullName());
        r.setStatus(user.getStatus().name());
        r.setRoleIds(userRoleRepository.findByUserId(user.getUserId()).stream().map(TenantUserRole::getRoleId).toList());
        return r;
    }
}
