package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefPaymentMethodAccepted;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefPaymentMethodAcceptedRepository extends JpaRepository<RefPaymentMethodAccepted, String> {
    List<RefPaymentMethodAccepted> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
