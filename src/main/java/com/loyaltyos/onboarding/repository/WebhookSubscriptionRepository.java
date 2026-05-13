package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {
    Optional<WebhookSubscription> findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(String tenantId);
    Optional<WebhookSubscription> findFirstByTenantIdOrderByCreatedAtDesc(String tenantId);
    long countByTenantIdAndActiveTrue(String tenantId);
}

