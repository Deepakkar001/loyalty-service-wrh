package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.AccessControlAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessControlAuditLogRepository extends JpaRepository<AccessControlAuditLog, Long> {

    List<AccessControlAuditLog> findTop20ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
