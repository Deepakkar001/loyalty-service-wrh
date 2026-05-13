package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.RefBusinessCategory;
import com.loyaltyos.onboarding.domain.enums.BusinessCategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefBusinessCategoryRepository extends JpaRepository<RefBusinessCategory, String> {

    /**
     * Used by the public {@code /api/v1/onboarding/metadata} endpoint. Only rows that
     * have been admin-approved (and not retired) are exposed in tenant dropdowns.
     */
    List<RefBusinessCategory> findByStatusAndActiveTrueOrderBySortOrderAscLabelAsc(BusinessCategoryStatus status);

    /** Admin moderation queue. */
    List<RefBusinessCategory> findByStatusOrderBySortOrderAscLabelAsc(BusinessCategoryStatus status);

    /** Full admin list across all statuses. */
    List<RefBusinessCategory> findAllByOrderByStatusAscSortOrderAscLabelAsc();

    /**
     * Backwards-compatible accessor preserved for any caller that still needs the
     * "active + any-status" view (kept to avoid silent regressions in existing
     * non-public flows). Prefer {@link #findByStatusAndActiveTrueOrderBySortOrderAscLabelAsc(BusinessCategoryStatus)}.
     */
    List<RefBusinessCategory> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
