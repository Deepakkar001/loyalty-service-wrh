package com.loyaltyos.access.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.access.dto.CreateRoleRequest;
import com.loyaltyos.access.dto.RoleResponse;
import com.loyaltyos.access.dto.RoleTemplateResponse;
import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.entity.TenantRole;
import com.loyaltyos.access.entity.TenantRoleTemplate;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantRoleTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantRoleService {

    private final TenantRoleRepository roleRepository;
    private final TenantRoleTemplateRepository templateRepository;
    private final PrivilegeGrantRepository grantRepository;
    private final ObjectMapper objectMapper;
    private final AccessControlAuditService auditService;

    public TenantRoleService(
        TenantRoleRepository roleRepository,
        TenantRoleTemplateRepository templateRepository,
        PrivilegeGrantRepository grantRepository,
        ObjectMapper objectMapper,
        AccessControlAuditService auditService
    ) {
        this.roleRepository = roleRepository;
        this.templateRepository = templateRepository;
        this.grantRepository = grantRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(String tenantId) {
        return roleRepository.findByTenantIdOrderByRoleNameAsc(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleTemplateResponse> listTemplates() {
        return templateRepository.findAll().stream().map(t -> {
            RoleTemplateResponse r = new RoleTemplateResponse();
            r.setTemplateKey(t.getTemplateKey());
            r.setRoleName(t.getRoleName());
            r.setDescription(t.getDescription());
            return r;
        }).toList();
    }

    @Transactional
    public RoleResponse createRole(String tenantId, CreateRoleRequest request) {
        if (roleRepository.existsByTenantIdAndRoleName(tenantId, request.getRoleName())) {
            throw new IllegalArgumentException("Role name already exists");
        }
        TenantRole role = new TenantRole();
        role.setRoleId(UUID.randomUUID().toString());
        role.setTenantId(tenantId);
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(request.getDescription());
        role.setSystem(false);
        role.setTemplateKey(request.getTemplateKey());
        roleRepository.save(role);

        if (request.getTemplateKey() != null && !request.getTemplateKey().isBlank()) {
            templateRepository.findById(request.getTemplateKey()).ifPresent(template -> {
                List<String> keys = parsePermissionKeys(template.getPermissionKeys());
                for (String key : keys) {
                    PrivilegeGrant grant = new PrivilegeGrant();
                    grant.setTenantId(tenantId);
                    grant.setSubjectType(GrantSubjectType.ROLE);
                    grant.setSubjectId(role.getRoleId());
                    grant.setPermissionKey(key);
                    grant.setEffect(GrantEffect.GRANT);
                    grantRepository.save(grant);
                }
            });
        }
        auditService.logFromSecurityContext(tenantId, "ROLE_CREATED",
            Map.of("roleId", role.getRoleId(), "roleName", role.getRoleName()));
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(String tenantId, String roleId) {
        TenantRole role = roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        if (role.isSystem()) {
            throw new IllegalArgumentException("Cannot delete system role");
        }
        grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(tenantId, GrantSubjectType.ROLE, roleId);
        roleRepository.delete(role);
        auditService.logFromSecurityContext(tenantId, "ROLE_DELETED",
            Map.of("roleId", roleId, "roleName", role.getRoleName()));
    }

    private RoleResponse toResponse(TenantRole role) {
        RoleResponse r = new RoleResponse();
        r.setRoleId(role.getRoleId());
        r.setRoleName(role.getRoleName());
        r.setDescription(role.getDescription());
        r.setSystem(role.isSystem());
        r.setTemplateKey(role.getTemplateKey());
        return r;
    }

    private List<String> parsePermissionKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
