package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.CampaignTargetCustomer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignTargetCustomerRepository extends JpaRepository<CampaignTargetCustomer, Long> {

    boolean existsByTenantIdAndCampaignUidAndCustomerId(String tenantId, String campaignUid, String customerId);

    long countByTenantIdAndCampaignUid(String tenantId, String campaignUid);

    Optional<CampaignTargetCustomer> findByTenantIdAndCampaignUidAndCustomerId(
        String tenantId,
        String campaignUid,
        String customerId
    );

    void deleteByTenantIdAndCampaignUidAndCustomerId(String tenantId, String campaignUid, String customerId);

    @Query(
        """
        SELECT c FROM CampaignTargetCustomer c
        WHERE c.tenantId = :tenantId AND c.campaignUid = :campaignUid
          AND (:search IS NULL OR :search = '' OR LOWER(c.customerId) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.addedAt DESC
        """
    )
    Page<CampaignTargetCustomer> search(
        @Param("tenantId") String tenantId,
        @Param("campaignUid") String campaignUid,
        @Param("search") String search,
        Pageable pageable
    );
}
