package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefSettlementFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefSettlementFrequencyRepository extends JpaRepository<RefSettlementFrequency, String> {
    List<RefSettlementFrequency> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
