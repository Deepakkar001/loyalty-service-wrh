package com.loyaltyos.access.service;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.dto.MeAccessResponse;
import com.loyaltyos.access.dto.NavGroupDto;
import com.loyaltyos.access.dto.NavItemDto;
import com.loyaltyos.access.entity.AccessModule;
import com.loyaltyos.access.entity.AccessNavItem;
import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.repository.AccessModuleActionRepository;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.AccessNavItemRepository;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.repository.TenantUserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessResolutionService {

    private final AccessProperties accessProperties;
    private final TenantUserRepository userRepository;
    private final TenantUserRoleRepository userRoleRepository;
    private final TenantRoleRepository roleRepository;
    private final TenantModuleEntitlementRepository entitlementRepository;
    private final PrivilegeGrantRepository grantRepository;
    private final AccessModuleRepository moduleRepository;
    private final AccessNavItemRepository navItemRepository;
    private final AccessModuleActionRepository moduleActionRepository;

    public AccessResolutionService(
        AccessProperties accessProperties,
        TenantUserRepository userRepository,
        TenantUserRoleRepository userRoleRepository,
        TenantRoleRepository roleRepository,
        TenantModuleEntitlementRepository entitlementRepository,
        PrivilegeGrantRepository grantRepository,
        AccessModuleRepository moduleRepository,
        AccessNavItemRepository navItemRepository,
        AccessModuleActionRepository moduleActionRepository
    ) {
        this.accessProperties = accessProperties;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.entitlementRepository = entitlementRepository;
        this.grantRepository = grantRepository;
        this.moduleRepository = moduleRepository;
        this.navItemRepository = navItemRepository;
        this.moduleActionRepository = moduleActionRepository;
    }

    @Transactional(readOnly = true)
    public MeAccessResponse resolveForUser(String tenantId, String tenantUserId) {
        TenantUser user = userRepository.findById(tenantUserId)
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> entitledModules = entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .map(TenantModuleEntitlement::getModuleKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> candidatePermissions = moduleActionRepository.findByModuleKeyIn(entitledModules).stream()
            .map(ma -> ma.getPermissionKey())
            .collect(Collectors.toSet());

        Set<String> effective = resolveEffectivePermissions(tenantId, tenantUserId, candidatePermissions);

        MeAccessResponse response = new MeAccessResponse();
        response.setTenantId(tenantId);
        response.setTenantUserId(tenantUserId);
        response.setSessionVersion(user.getSessionVersion());
        response.setPermissions(new ArrayList<>(effective));
        response.setEntitledModules(new ArrayList<>(entitledModules));
        response.setNavGroups(buildNavGroups(entitledModules, effective));
        response.setModulesConfigured(entitlementRepository.countByTenantId(tenantId) > 0);
        response.setDynamicNavEnabled(accessProperties.getDynamicNav().isEnabled());
        return response;
    }

    public Set<String> resolveEffectivePermissions(String tenantId, String tenantUserId, Set<String> candidatePermissions) {
        List<String> roleIds = userRoleRepository.findByUserId(tenantUserId).stream()
            .map(ur -> ur.getRoleId())
            .toList();

        Set<String> rolePerms = new HashSet<>();
        if (!roleIds.isEmpty()) {
            grantRepository.findByTenantIdAndSubjectTypeAndSubjectIdIn(
                tenantId, GrantSubjectType.ROLE, roleIds).stream()
                .filter(g -> g.getEffect() == GrantEffect.GRANT)
                .map(PrivilegeGrant::getPermissionKey)
                .forEach(rolePerms::add);
        }

        Set<String> userGrants = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
            tenantId, GrantSubjectType.USER, tenantUserId).stream()
            .filter(g -> g.getEffect() == GrantEffect.GRANT)
            .map(PrivilegeGrant::getPermissionKey)
            .collect(Collectors.toSet());

        Set<String> userDenies = grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
            tenantId, GrantSubjectType.USER, tenantUserId).stream()
            .filter(g -> g.getEffect() == GrantEffect.DENY)
            .map(PrivilegeGrant::getPermissionKey)
            .collect(Collectors.toSet());

        Set<String> effective = new HashSet<>(rolePerms);
        effective.addAll(userGrants);
        effective.removeAll(userDenies);

        if (effective.isEmpty() && roleRepository.findByTenantIdAndSystemTrue(tenantId).isPresent()) {
            effective.addAll(candidatePermissions);
        }

        effective.retainAll(candidatePermissions);
        return effective;
    }

    public boolean hasPermission(String tenantId, String tenantUserId, String permissionKey) {
        Set<String> entitledModules = entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .map(TenantModuleEntitlement::getModuleKey)
            .collect(Collectors.toSet());
        Set<String> candidates = moduleActionRepository.findByModuleKeyIn(entitledModules).stream()
            .map(ma -> ma.getPermissionKey())
            .collect(Collectors.toSet());
        return resolveEffectivePermissions(tenantId, tenantUserId, candidates).contains(permissionKey);
    }

    public boolean isModuleEntitled(String tenantId, String moduleKey) {
        return entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .anyMatch(e -> moduleKey.equals(e.getModuleKey()));
    }

    private List<NavGroupDto> buildNavGroups(Set<String> entitledModules, Set<String> permissions) {
        List<AccessNavItem> navItems = navItemRepository.findByModuleKeyInOrderBySortOrderAsc(entitledModules);
        Map<String, AccessModule> modulesByKey = moduleRepository.findByActiveTrueOrderBySortOrderAsc().stream()
            .collect(Collectors.toMap(AccessModule::getModuleKey, m -> m, (a, b) -> a, LinkedHashMap::new));

        Map<String, List<NavItemDto>> grouped = new LinkedHashMap<>();
        for (AccessNavItem item : navItems) {
            if (!permissions.contains(item.getRequiredPermissionKey())) {
                continue;
            }
            AccessModule module = modulesByKey.get(item.getModuleKey());
            String section = module != null ? module.getNavSection() : "Other";
            grouped.computeIfAbsent(section, k -> new ArrayList<>()).add(new NavItemDto(
                item.getRoutePath(),
                item.getLabelKey(),
                item.getIconKey(),
                item.getModuleKey(),
                item.isRequiresOnboardingComplete()
            ));
        }

        return grouped.entrySet().stream()
            .map(e -> new NavGroupDto(e.getKey(), e.getValue()))
            .toList();
    }
}
