package com.loyaltyos.integration.repository;

import com.loyaltyos.integration.entity.CredentialAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialAccessLogRepository extends JpaRepository<CredentialAccessLog, Long> {
}
