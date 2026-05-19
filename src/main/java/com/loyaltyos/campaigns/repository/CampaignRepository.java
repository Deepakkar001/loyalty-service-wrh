package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByTenantIdAndCampaignUid(String tenantId, String campaignUid);

    List<Campaign> findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc(
        String tenantId,
        String programmeUid
    );

    List<Campaign> findByTenantIdAndProgrammeUidAndStatusOrderByPriorityDescCreatedAtDesc(
        String tenantId,
        String programmeUid,
        CampaignStatus status
    );

    List<Campaign> findByTenantIdOrderByPriorityDescCreatedAtDesc(String tenantId);

    List<Campaign> findByTenantIdAndStatusOrderByPriorityDescCreatedAtDesc(String tenantId, CampaignStatus status);

    List<Campaign> findByTenantIdAndProgrammeUidAndMutualExclGroup(
        String tenantId,
        String programmeUid,
        String mutualExclGroup
    );

    /**
     * Column order matches {@code idx_tenant_prog_status_window}
     * ({@code tenant_id, programme_uid, status, valid_from, valid_until}).
     */
    @Query(
        value = """
            SELECT *
            FROM campaigns c
            WHERE c.tenant_id = :tenantId
              AND c.programme_uid = :programmeUid
              AND c.status = 'ACTIVE'
              AND c.valid_from <= :now
              AND c.valid_until >= :now
              AND (
                c.trigger_event_type = :eventType
                OR FIND_IN_SET(:eventType, REPLACE(c.trigger_event_type, ' ', '')) > 0
              )
            ORDER BY c.priority DESC
            """,
        nativeQuery = true
    )
    List<Campaign> findActiveForEligibility(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("eventType") String eventType,
        @Param("now") Instant now
    );
}
