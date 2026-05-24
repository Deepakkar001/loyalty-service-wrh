package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.entity.SandboxTestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SandboxTestEventRepository extends JpaRepository<SandboxTestEvent, Long> {
}
