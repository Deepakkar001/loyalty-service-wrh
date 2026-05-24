package com.loyaltyos.rewards.repository;

import com.loyaltyos.rewards.entity.PointsExpiryJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsExpiryJobRepository extends JpaRepository<PointsExpiryJob, Long> {
}
