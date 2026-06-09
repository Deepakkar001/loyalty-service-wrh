package com.loyaltyos.voucher.repository;

import com.loyaltyos.voucher.entity.VoucherBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherBatchRepository extends JpaRepository<VoucherBatch, Long> {

    Optional<VoucherBatch> findByBatchUid(String batchUid);

    Optional<VoucherBatch> findByTenantIdAndFileSha256(String tenantId, String fileSha256);

    List<VoucherBatch> findByTenantIdOrderByUploadedAtDesc(String tenantId);

    List<VoucherBatch> findByTenantIdAndProgrammeUidOrderByUploadedAtDesc(String tenantId, String programmeUid);

    List<VoucherBatch> findByTenantIdAndProgrammeUidAndCatalogRewardUidOrderByUploadedAtDesc(
        String tenantId,
        String programmeUid,
        String catalogRewardUid
    );
}
