package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefContractDuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefContractDurationRepository extends JpaRepository<RefContractDuration, String> {
    List<RefContractDuration> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
