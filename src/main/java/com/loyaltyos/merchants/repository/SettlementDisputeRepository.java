package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.SettlementDispute;
import com.loyaltyos.merchants.enums.SettlementDisputeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDisputeRepository extends JpaRepository<SettlementDispute, Long> {

    Optional<SettlementDispute> findByDisputeUid(String disputeUid);

    List<SettlementDispute> findByStatusOrderByCreatedAtDesc(SettlementDisputeStatus status);
}
