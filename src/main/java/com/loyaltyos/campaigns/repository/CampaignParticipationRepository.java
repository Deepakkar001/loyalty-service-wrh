package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.CampaignParticipation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignParticipationRepository extends JpaRepository<CampaignParticipation, Long> {

    long countByTenantIdAndCampaignUid(String tenantId, String campaignUid);

    long countByTenantIdAndCampaignUidAndCustomerId(String tenantId, String campaignUid, String customerId);

    @Query(
        """
        SELECT COALESCE(SUM(p.pointsAwarded), 0)
        FROM CampaignParticipation p
        WHERE p.tenantId = :tenantId AND p.campaignUid = :campaignUid
        """
    )
    BigDecimal sumPointsAwarded(@Param("tenantId") String tenantId, @Param("campaignUid") String campaignUid);

    @Query(
        """
        SELECT COALESCE(SUM(p.cashbackAmount), 0)
        FROM CampaignParticipation p
        WHERE p.tenantId = :tenantId AND p.campaignUid = :campaignUid
        """
    )
    BigDecimal sumCashbackRecorded(@Param("tenantId") String tenantId, @Param("campaignUid") String campaignUid);

    @Query(
        """
        SELECT COUNT(DISTINCT p.customerId)
        FROM CampaignParticipation p
        WHERE p.tenantId = :tenantId AND p.campaignUid = :campaignUid
        """
    )
    long countDistinctCustomers(@Param("tenantId") String tenantId, @Param("campaignUid") String campaignUid);

    List<CampaignParticipation> findByTenantIdAndCampaignUidOrderByParticipatedAtDesc(
        String tenantId,
        String campaignUid,
        Pageable pageable
    );
}
