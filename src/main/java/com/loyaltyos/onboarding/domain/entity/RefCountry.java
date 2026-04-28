package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ref_country")
@Getter
@Setter
public class RefCountry {

    @Id
    @Column(name = "code", length = 2, nullable = false, updatable = false, columnDefinition = "CHAR(2)")
    private String code;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}

