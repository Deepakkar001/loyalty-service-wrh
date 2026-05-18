package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.CampaignResolutionLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignResolutionLogRepository extends JpaRepository<CampaignResolutionLog, Long> {

    Optional<CampaignResolutionLog> findByTenantIdAndEventId(String tenantId, String eventId);
}
