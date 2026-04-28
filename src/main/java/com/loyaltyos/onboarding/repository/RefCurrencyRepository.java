package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefCurrencyRepository extends JpaRepository<RefCurrency, String> {
    List<RefCurrency> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
