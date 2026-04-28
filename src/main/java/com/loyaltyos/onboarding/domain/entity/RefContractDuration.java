package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ref_contract_duration")
@Getter
@Setter
public class RefContractDuration {
    @Id
    @Column(name = "code", length = 10, nullable = false, updatable = false)
    private String code;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}
