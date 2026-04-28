package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefBusinessModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefBusinessModelRepository extends JpaRepository<RefBusinessModel, String> {
    List<RefBusinessModel> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
