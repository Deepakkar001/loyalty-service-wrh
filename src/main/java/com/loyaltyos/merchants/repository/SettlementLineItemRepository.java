package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.SettlementLineItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementLineItemRepository extends JpaRepository<SettlementLineItem, Long> {

    Optional<SettlementLineItem> findByLineItemUid(String lineItemUid);

    List<SettlementLineItem> findByCycleIdOrderByCreatedAtAsc(Long cycleId);
}
