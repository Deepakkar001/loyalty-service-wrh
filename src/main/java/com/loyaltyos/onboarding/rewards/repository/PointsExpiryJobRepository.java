package com.loyaltyos.onboarding.rewards.repository;

import com.loyaltyos.onboarding.rewards.entity.PointsExpiryJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsExpiryJobRepository extends JpaRepository<PointsExpiryJob, Long> {
}
