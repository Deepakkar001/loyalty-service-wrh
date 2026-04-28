package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefBusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefBusinessCategoryRepository extends JpaRepository<RefBusinessCategory, String> {
    List<RefBusinessCategory> findByActiveTrueOrderBySortOrderAscLabelAsc();
}

