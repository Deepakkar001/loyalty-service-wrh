package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.entity.CampaignTargetUpload;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignTargetUploadRepository extends JpaRepository<CampaignTargetUpload, Long> {

    Optional<CampaignTargetUpload> findByTenantIdAndUploadUid(String tenantId, String uploadUid);

    /**
     * Latest upload batch for this campaign and file content (SHA-256).
     * Scoped to campaign so re-uploads across campaigns or duplicate history rows do not break Optional lookup.
     */
    Optional<CampaignTargetUpload> findTopByTenantIdAndCampaignUidAndFileSha256OrderByUploadedAtDesc(
        String tenantId,
        String campaignUid,
        String fileSha256
    );

    List<CampaignTargetUpload> findByTenantIdAndCampaignUidOrderByUploadedAtDesc(
        String tenantId,
        String campaignUid
    );
}
