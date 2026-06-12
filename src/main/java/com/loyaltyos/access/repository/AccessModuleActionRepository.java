package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.AccessModuleAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccessModuleActionRepository extends JpaRepository<AccessModuleAction, Long> {

    List<AccessModuleAction> findByModuleKeyIn(Collection<String> moduleKeys);

    List<AccessModuleAction> findByAssignableTrue();

    List<AccessModuleAction> findAllByOrderByModuleKeyAscActionKeyAsc();
}
