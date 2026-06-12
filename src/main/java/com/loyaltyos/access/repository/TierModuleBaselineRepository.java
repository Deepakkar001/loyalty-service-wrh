package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TierModuleBaseline;
import com.loyaltyos.access.entity.TierModuleBaselineId;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierModuleBaselineRepository extends JpaRepository<TierModuleBaseline, TierModuleBaselineId> {

    List<TierModuleBaseline> findBySubscriptionTier(SubscriptionTier subscriptionTier);
}
