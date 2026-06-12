package com.loyaltyos.access.service;

import com.loyaltyos.access.dto.AssignPrivilegesRequest;
import com.loyaltyos.access.dto.PrivilegeMatrixResponse;
import com.loyaltyos.access.dto.PrivilegeRowDto;
import com.loyaltyos.access.entity.AccessModule;
import com.loyaltyos.access.entity.AccessModuleAction;
import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.repository.AccessModuleActionRepository;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PrivilegeAssignmentService {

    private final TenantRoleRepository roleRepository;
    private final TenantUserRepository userRepository;
    private final TenantModuleEntitlementRepository entitlementRepository;
    private final AccessModuleRepository moduleRepository;
    private final AccessModuleActionRepository moduleActionRepository;
    private final PrivilegeGrantRepository grantRepository;
    private final AccessProvisioningService provisioningService;
    private final AccessControlAuditService auditService;

    public PrivilegeAssignmentService(
        TenantRoleRepository roleRepository,
        TenantUserRepository userRepository,
        TenantModuleEntitlementRepository entitlementRepository,
        AccessModuleRepository moduleRepository,
        AccessModuleActionRepository moduleActionRepository,
        PrivilegeGrantRepository grantRepository,
        AccessProvisioningService provisioningService,
        AccessControlAuditService auditService
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.entitlementRepository = entitlementRepository;
        this.moduleRepository = moduleRepository;
        this.moduleActionRepository = moduleActionRepository;
        this.grantRepository = grantRepository;
        this.provisioningService = provisioningService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PrivilegeMatrixResponse loadMatrix(String tenantId, String roleId, String userId) {
        roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<String> entitledModules = entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .map(TenantModuleEntitlement::getModuleKey)
            .collect(Collectors.toSet());

        Set<String> roleGrants = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
            tenantId, GrantSubjectType.ROLE, roleId).stream()
            .filter(g -> g.getEffect() == GrantEffect.GRANT)
            .map(PrivilegeGrant::getPermissionKey)
            .collect(Collectors.toSet());

        Set<String> userGrants;
        Set<String> userDenies;
        if (userId != null && !userId.isBlank() && !"select".equals(userId)) {
            userRepository.findById(userId)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            userGrants = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
                tenantId, GrantSubjectType.USER, userId).stream()
                .filter(g -> g.getEffect() == GrantEffect.GRANT)
                .map(PrivilegeGrant::getPermissionKey)
                .collect(Collectors.toSet());
            userDenies = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
                tenantId, GrantSubjectType.USER, userId).stream()
                .filter(g -> g.getEffect() == GrantEffect.DENY)
                .map(PrivilegeGrant::getPermissionKey)
                .collect(Collectors.toSet());
        } else {
            userGrants = Set.of();
            userDenies = Set.of();
        }

        List<AccessModuleAction> actions = moduleActionRepository.findByModuleKeyIn(entitledModules);
        List<String> actionKeys = actions.stream().map(AccessModuleAction::getActionKey).distinct().sorted().toList();

        Set<String> effectiveSelected = new HashSet<>(roleGrants);
        effectiveSelected.addAll(userGrants);
        effectiveSelected.removeAll(userDenies);

        boolean userMode = userId != null && !userId.isBlank() && !"select".equals(userId);

        List<PrivilegeRowDto> rows = actions.stream().map(ma -> {
            AccessModule module = moduleRepository.findById(ma.getModuleKey()).orElse(null);
            PrivilegeRowDto row = new PrivilegeRowDto();
            row.setModuleKey(ma.getModuleKey());
            row.setModuleName(module != null ? module.getDisplayName() : ma.getModuleKey());
            row.setNavSection(module != null ? module.getNavSection() : "Other");
            row.setActionKey(ma.getActionKey());
            row.setPermissionKey(ma.getPermissionKey());
            row.setAssignable(ma.isAssignable());
            boolean roleHas = roleGrants.contains(ma.getPermissionKey());
            boolean selected = userMode
                ? effectiveSelected.contains(ma.getPermissionKey())
                : roleHas;
            row.setSelected(selected);
            row.setInheritedFromRole(userMode && roleHas);
            row.setDenied(userMode && userDenies.contains(ma.getPermissionKey()));
            return row;
        }).toList();

        PrivilegeMatrixResponse response = new PrivilegeMatrixResponse();
        response.setRoleId(roleId);
        response.setUserId(userId);
        response.setActionKeys(actionKeys);
        response.setRows(rows);
        return response;
    }

    @Transactional
    public PrivilegeMatrixResponse assign(String tenantId, AssignPrivilegesRequest request) {
        String roleId = request.getRoleId();
        String userId = request.getUserId();
        roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<String> entitledPermissions = moduleActionRepository.findByModuleKeyIn(
            entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
                .map(TenantModuleEntitlement::getModuleKey).collect(Collectors.toSet())
        ).stream().map(AccessModuleAction::getPermissionKey).collect(Collectors.toSet());

        Set<String> requested = new HashSet<>(request.getPermissionKeys() != null ? request.getPermissionKeys() : List.of());
        requested.retainAll(entitledPermissions);

        boolean userMode = userId != null && !userId.isBlank() && !"select".equals(userId);

        if (userMode) {
            userRepository.findById(userId)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(tenantId, GrantSubjectType.USER, userId);

            Set<String> roleGrants = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
                tenantId, GrantSubjectType.ROLE, roleId).stream()
                .filter(g -> g.getEffect() == GrantEffect.GRANT)
                .map(PrivilegeGrant::getPermissionKey)
                .collect(Collectors.toSet());

            for (String perm : entitledPermissions) {
                boolean roleHas = roleGrants.contains(perm);
                boolean userWants = requested.contains(perm);
                if (roleHas && !userWants) {
                    saveGrant(tenantId, GrantSubjectType.USER, userId, perm, GrantEffect.DENY);
                } else if (!roleHas && userWants) {
                    saveGrant(tenantId, GrantSubjectType.USER, userId, perm, GrantEffect.GRANT);
                }
            }
            provisioningService.bumpSessionVersion(tenantId);
        } else {
            grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(tenantId, GrantSubjectType.ROLE, roleId);
            for (String perm : requested) {
                saveGrant(tenantId, GrantSubjectType.ROLE, roleId, perm, GrantEffect.GRANT);
            }
            provisioningService.bumpSessionVersion(tenantId);
        }

        auditService.logFromSecurityContext(tenantId, userMode ? "USER_PRIVILEGES_ASSIGNED" : "ROLE_PRIVILEGES_ASSIGNED",
            Map.of("roleId", roleId, "userId", userId, "permissionCount", requested.size()));

        return loadMatrix(tenantId, roleId, userId);
    }

    private void saveGrant(String tenantId, GrantSubjectType type, String subjectId, String perm, GrantEffect effect) {
        PrivilegeGrant g = new PrivilegeGrant();
        g.setTenantId(tenantId);
        g.setSubjectType(type);
        g.setSubjectId(subjectId);
        g.setPermissionKey(perm);
        g.setEffect(effect);
        grantRepository.save(g);
    }
}
