package com.loyaltyos.referrals.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.loyaltyos.referrals.entity.ReferralCode;
import com.loyaltyos.referrals.enums.ReferralCodeStatus;
import com.loyaltyos.referrals.repository.ReferralCodeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferralCodeServiceTest {

    @Mock private ReferralCodeRepository codeRepository;
    @Mock private ReferralProgrammeService programmeService;
    @Mock private ReferralEligibilityService eligibilityService;
    @Mock private ReferralAuditService auditService;

    private ReferralCodeService codeService;

    @BeforeEach
    void setUp() {
        codeService = new ReferralCodeService(
            codeRepository, programmeService, eligibilityService, auditService
        );
    }

    @Test
    void getOrGenerate_returnsExisting() {
        ReferralCode existing = new ReferralCode();
        existing.setCode("ABC123");
        existing.setCustomerId("c1");
        when(codeRepository.findByTenantIdAndProgrammeUidAndCustomerIdAndStatus(
            "t1", "default", "c1", ReferralCodeStatus.ACTIVE
        )).thenReturn(Optional.of(existing));

        ReferralCode out = codeService.getOrGenerateCode("t1", "c1", "default");
        assertThat(out.getCode()).isEqualTo("ABC123");
    }
}
