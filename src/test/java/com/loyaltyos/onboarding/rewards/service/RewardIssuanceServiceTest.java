package com.loyaltyos.onboarding.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loyaltyos.onboarding.analytics.service.TierHistoryService;
import com.loyaltyos.onboarding.analytics.service.TierResolver;
import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import com.loyaltyos.onboarding.rules.repository.EarnRuleRepository;
import com.loyaltyos.onboarding.rewards.config.RewardEngineProperties;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueRequest;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueResponse;
import com.loyaltyos.onboarding.rewards.exception.RewardIssuanceValidationException;
import com.loyaltyos.onboarding.rewards.exception.RewardPartialIdempotencyException;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RewardIssuanceServiceTest {

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @Mock
    private CustomerBalanceCacheRepository balanceCacheRepository;

    @Mock
    private EarnRuleRepository earnRuleRepository;

    @Mock
    private RewardIssuanceAuditService rewardIssuanceAuditService;

    @Mock
    private RewardEngineProperties rewardEngineProperties;

    @Mock
    private TierResolver tierResolver;

    @Mock
    private TierHistoryService tierHistoryService;

    @InjectMocks
    private RewardIssuanceService service;

    @BeforeEach
    void setUp() {
        lenient().when(earnRuleRepository.findByTenantIdAndProgrammeUidAndRuleUid("t1", "default", "rule-a"))
            .thenReturn(Optional.of(mockRule(10L, "rule-a")));
        lenient().when(rewardEngineProperties.getDefaultCreditExpiryMonths()).thenReturn(24);
    }

    private static EarnRule mockRule(long id, String uid) {
        EarnRule r = new EarnRule();
        r.setId(id);
        r.setRuleUid(uid);
        r.setName("Test");
        r.setTriggerEventType("PURCHASE");
        return r;
    }

    @Test
    void issue_insertsLedgerAndIncrementsBalance() {
        RewardIssueRequest req = new RewardIssueRequest();
        req.setProgrammeUid("default");
        req.setCustomerId("c1");
        req.setEventId("evt-1");
        RewardIssueCommandDto cmd = new RewardIssueCommandDto();
        cmd.setIdempotencyKey("idem-1");
        cmd.setSourceRuleUid("rule-a");
        cmd.setPointsToAward(new BigDecimal("12.5000"));
        req.setRewardCommands(List.of(cmd));

        when(pointsLedgerRepository.findByTenantIdAndCustomerIdAndIdempotencyKeyIn(eq("t1"), eq("c1"), anyList()))
            .thenReturn(List.of());

        PointsLedger saved = PointsLedger.builder()
            .id(99L)
            .tenantId("t1")
            .customerId("c1")
            .programmeUid("default")
            .idempotencyKey("idem-1")
            .entryType(LedgerEntryType.CREDIT)
            .points(new BigDecimal("12.5000"))
            .sourceRuleId(10L)
            .sourceEventId("evt-1")
            .build();
        when(pointsLedgerRepository.saveAll(anyList())).thenReturn(List.of(saved));

        RewardIssueResponse res = service.issue("t1", req);

        assertThat(res.getLedgerRowsCreated()).isEqualTo(1);
        assertThat(res.getTotalPointsIssued()).isEqualByComparingTo("12.5000");
        assertThat(res.isIdempotentReplay()).isFalse();

        verify(balanceCacheRepository).incrementBalance("t1", "default", "c1", new BigDecimal("12.5000"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointsLedger>> cap = ArgumentCaptor.forClass(List.class);
        verify(pointsLedgerRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(1);
        assertThat(cap.getValue().getFirst().getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(cap.getValue().getFirst().getExpiresAt()).isNotNull();
    }

    @Test
    void issue_emptyCommands_skipsPersistence() {
        RewardIssueRequest req = new RewardIssueRequest();
        req.setProgrammeUid("default");
        req.setCustomerId("c1");
        req.setEventId("evt-1");
        req.setRewardCommands(List.of());

        RewardIssueResponse res = service.issue("t1", req);

        assertThat(res.getLedgerRowsCreated()).isEqualTo(0);
        verify(pointsLedgerRepository, never()).saveAll(anyList());
        verify(balanceCacheRepository, never()).incrementBalance(any(), any(), any(), any());
    }

    @Test
    void issue_rejectsDuplicateKeysInPayload() {
        RewardIssueRequest req = new RewardIssueRequest();
        req.setProgrammeUid("default");
        req.setCustomerId("c1");
        req.setEventId("evt-1");
        RewardIssueCommandDto a = new RewardIssueCommandDto();
        a.setIdempotencyKey("same");
        a.setSourceRuleUid("rule-a");
        a.setPointsToAward(BigDecimal.ONE);
        RewardIssueCommandDto b = new RewardIssueCommandDto();
        b.setIdempotencyKey("same");
        b.setSourceRuleUid("rule-a");
        b.setPointsToAward(BigDecimal.ONE);
        req.setRewardCommands(List.of(a, b));

        assertThatThrownBy(() -> service.issue("t1", req))
            .isInstanceOf(RewardIssuanceValidationException.class)
            .hasMessageContaining("Duplicate idempotencyKey");
    }

    @Test
    void issue_campaignCommand_setsSourceCampaignId() {
        RewardIssueRequest req = new RewardIssueRequest();
        req.setProgrammeUid("default");
        req.setCustomerId("c1");
        req.setEventId("evt-camp");
        RewardIssueCommandDto cmd = new RewardIssueCommandDto();
        cmd.setIdempotencyKey("camp:camp-1:evt-camp:POINTS_BONUS");
        cmd.setSourceCampaignUid("camp-1");
        cmd.setPointsToAward(new BigDecimal("10"));
        req.setRewardCommands(List.of(cmd));

        when(pointsLedgerRepository.findByTenantIdAndCustomerIdAndIdempotencyKeyIn(eq("t1"), eq("c1"), anyList()))
            .thenReturn(List.of());

        PointsLedger saved = PointsLedger.builder()
            .id(100L)
            .tenantId("t1")
            .customerId("c1")
            .programmeUid("default")
            .idempotencyKey(cmd.getIdempotencyKey())
            .entryType(LedgerEntryType.CREDIT)
            .points(new BigDecimal("10"))
            .sourceCampaignId("camp-1")
            .sourceEventId("evt-camp")
            .build();
        when(pointsLedgerRepository.saveAll(anyList())).thenReturn(List.of(saved));

        service.issue("t1", req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointsLedger>> cap = ArgumentCaptor.forClass(List.class);
        verify(pointsLedgerRepository).saveAll(cap.capture());
        assertThat(cap.getValue().getFirst().getSourceCampaignId()).isEqualTo("camp-1");
        assertThat(cap.getValue().getFirst().getSourceRuleId()).isNull();
        verify(earnRuleRepository, never()).findByTenantIdAndProgrammeUidAndRuleUid(any(), any(), any());
    }

    @Test
    void issue_partialIdempotency_throws() {
        RewardIssueRequest req = new RewardIssueRequest();
        req.setProgrammeUid("default");
        req.setCustomerId("c1");
        req.setEventId("evt-1");
        RewardIssueCommandDto a = new RewardIssueCommandDto();
        a.setIdempotencyKey("k1");
        a.setSourceRuleUid("rule-a");
        a.setPointsToAward(BigDecimal.ONE);
        RewardIssueCommandDto b = new RewardIssueCommandDto();
        b.setIdempotencyKey("k2");
        b.setSourceRuleUid("rule-a");
        b.setPointsToAward(BigDecimal.ONE);
        req.setRewardCommands(List.of(a, b));

        PointsLedger existing = new PointsLedger();
        existing.setId(1L);
        existing.setIdempotencyKey("k1");
        when(pointsLedgerRepository.findByTenantIdAndCustomerIdAndIdempotencyKeyIn(eq("t1"), eq("c1"), anyList()))
            .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.issue("t1", req))
            .isInstanceOf(RewardPartialIdempotencyException.class);
    }
}
