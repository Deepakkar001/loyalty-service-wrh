package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefBillingPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefBillingPaymentMethodRepository extends JpaRepository<RefBillingPaymentMethod, String> {
    List<RefBillingPaymentMethod> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
