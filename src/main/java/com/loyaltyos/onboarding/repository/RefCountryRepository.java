package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.entity.RefCountry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefCountryRepository extends JpaRepository<RefCountry, String> {
    List<RefCountry> findByActiveTrueOrderBySortOrderAscLabelAsc();
}

