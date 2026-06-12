package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.AccessAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessActionRepository extends JpaRepository<AccessAction, String> {

    List<AccessAction> findAllByOrderBySortOrderAsc();
}
