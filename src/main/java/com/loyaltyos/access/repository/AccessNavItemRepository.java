package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.AccessNavItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccessNavItemRepository extends JpaRepository<AccessNavItem, Long> {

    List<AccessNavItem> findByModuleKeyInOrderBySortOrderAsc(Collection<String> moduleKeys);

    List<AccessNavItem> findAllByOrderBySortOrderAsc();
}
