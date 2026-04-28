package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefAnnualRevenueRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefAnnualRevenueRangeRepository extends JpaRepository<RefAnnualRevenueRange, String> {
    List<RefAnnualRevenueRange> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
