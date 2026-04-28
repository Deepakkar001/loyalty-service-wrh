package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefTimezone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefTimezoneRepository extends JpaRepository<RefTimezone, String> {
    List<RefTimezone> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
