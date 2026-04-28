package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class PendingAgreementListItem {
    private String agreementUid;
    private String tenantId;
    private String companyName;
    private String tenantEmail;
    private String termsVersion;
    private LocalDate effectiveDate;
    private BigDecimal revenueSharePct;
    private SettlementFrequency settlementFrequency;
    private String signedByName;
    private String signedByEmail;
    private String signedByDesignation;
    private Instant signedAt;
    private AgreementStatus status;
    private Instant createdAt;
}
