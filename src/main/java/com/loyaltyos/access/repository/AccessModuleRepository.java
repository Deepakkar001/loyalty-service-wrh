package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.AccessModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessModuleRepository extends JpaRepository<AccessModule, String> {

    List<AccessModule> findByActiveTrueOrderBySortOrderAsc();
}
