package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_contract_duration")
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

    /** JPA requires a no-arg constructor. */
    public RefContractDuration() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
