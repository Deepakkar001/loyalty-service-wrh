package com.loyaltyos.campaigns.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.model.CampaignBuiltAward;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignResolutionLogRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.config.RewardEngineProperties;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.rewards.dto.RewardIssueResponse;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import com.loyaltyos.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.rules.service.ProgrammeEvaluationContext;
import com.loyaltyos.rules.service.ProgrammeRuleContextLoader;
import com.loyaltyos.rules.service.RuleEarningCapService;
import com.loyaltyos.rules.service.RuleEvaluationService;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.service.VoucherAutoIssueService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class CampaignOrchestrationServiceVoucherAutoIssueTest {

    @Mock private CampaignProperties campaignProperties;
    @Mock private ProgrammeService programmeService;
    @Mock private ProgrammeRuleContextLoader programmeRuleContextLoader;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private CampaignEligibilityService eligibilityService;
    @Mock private CampaignConflictResolver conflictResolver;
    @Mock private CampaignRewardCommandBuilder rewardCommandBuilder;
    @Mock private CampaignBudgetService budgetService;
    @Mock private RuleEvaluationService ruleEvaluationService;
    @Mock private RuleEarningCapService ruleEarningCapService;
    @Mock private RewardIssuanceService rewardIssuanceService;
    @Mock private RewardEngineProperties rewardEngineProperties;
    @Mock private CampaignJsonSupport jsonSupport;
    @Mock private CampaignParticipationRepository participationRepository;
    @Mock private CampaignResolutionLogRepository resolutionLogRepository;
    @Mock private VoucherAutoIssueService voucherAutoIssueService;
    @Mock private PlatformTransactionManager transactionManager;

    private CampaignOrchestrationService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(org.mockito.Mockito.mock(TransactionStatus.class));
        lenient().doAnswer(invocation -> null).when(transactionManager).commit(any());
        lenient().doAnswer(invocation -> null).when(transactionManager).rollback(any());

        service = new CampaignOrchestrationService(
            campaignProperties,
            programmeService,
            programmeRuleContextLoader,
            objectMapper,
            eligibilityService,
            conflictResolver,
            rewardCommandBuilder,
            budgetService,
            ruleEvaluationService,
            ruleEarningCapService,
            rewardIssuanceService,
            rewardEngineProperties,
            jsonSupport,
            participationRepository,
            resolutionLogRepository,
            voucherAutoIssueService,
            transactionManager
        );
    }

    @Test
    void process_autoIssuesVoucherWhenConfigured_butStillReturnsPointsSuccess() {
        when(campaignProperties.isResolutionLogEnabled()).thenReturn(false);

        ProgrammeEvaluationContext ctx = ProgrammeEvaluationContext.builder()
            .basePointsRate(BigDecimal.ONE)
            .resolvedTierMultiplier(BigDecimal.ONE)
            .build();
        when(programmeRuleContextLoader.load(eq("t1"), eq("default"), any())).thenReturn(ctx);
        when(programmeService.getActiveConfigOrNull(eq("t1"), eq("default"))).thenReturn(null);

        RuleEvaluationResponse ruleEval = new RuleEvaluationResponse();
        ruleEval.setSuccess(true);
        ruleEval.setFinalPointsAwarded(BigDecimal.ZERO);
        RuleEvaluationResponse.MatchedRuleInfo m = new RuleEvaluationResponse.MatchedRuleInfo();
        m.setRuleUid("r1");
        m.setRuleName("Rule 1");
        m.setPriority(10);
        ruleEval.setMatchedRules(List.of(m));

        RuleEvaluationResponse.CatalogGrantInfo g = new RuleEvaluationResponse.CatalogGrantInfo();
        g.setSourceRuleUid("r1");
        g.setRuleName("Rule 1");
        g.setCatalogRewardUid("zenzek_voucher");
        g.setIssueMode("AUTO_ISSUE_ON_EVENT");
        g.setSelectionMode("BY_POINTS");
        g.setPointsToRedeem(new BigDecimal("50"));
        g.setValid(true);
        ruleEval.setCatalogGrants(List.of(g));
        when(ruleEvaluationService.evaluate(eq("t1"), any(RuleEvaluateRequest.class))).thenReturn(ruleEval);

        when(rewardCommandBuilder.build(any(), any(), any(), any(), any())).thenReturn(List.<CampaignBuiltAward>of());

        RewardIssueResponse issue = new RewardIssueResponse();
        issue.setMessage("ok");
        issue.setIdempotentReplay(false);
        issue.setNewBalance(new BigDecimal("1000"));
        when(rewardIssuanceService.issue(eq("t1"), any())).thenReturn(issue);
        RewardBalanceResponse bal = new RewardBalanceResponse();
        bal.setTenantId("t1");
        bal.setProgrammeUid("default");
        bal.setCustomerId("cust_1");
        bal.setBalance(new BigDecimal("950"));
        when(rewardIssuanceService.getBalance(eq("t1"), eq("default"), eq("cust_1"))).thenReturn(bal);

        when(voucherAutoIssueService.isEnabled()).thenReturn(true);
        VoucherIssueResponse issued = new VoucherIssueResponse();
        issued.setStatus("SUCCESS");
        issued.setPointsRedeemed(new BigDecimal("50"));
        VoucherIssueResponse.VoucherDetails details = new VoucherIssueResponse.VoucherDetails();
        details.setCode("MD-100-A01");
        issued.setVoucher(details);
        when(voucherAutoIssueService.autoIssue(
            eq("t1"),
            eq("default"),
            eq("zenzek_voucher"),
            eq("cust_1"),
            eq("evt_1:r1"),
            eq(new BigDecimal("50")),
            eq(null)
        )).thenReturn(issued);

        LoyaltyEventProcessRequest req = new LoyaltyEventProcessRequest();
        req.setProgrammeUid("default");
        req.setEvaluationScope("PROGRAMME_RULES");
        req.setCustomerId("cust_1");
        req.setEventType("BILLPAY");
        req.setTransactionId("evt_1");
        req.setAmount(new BigDecimal("100"));

        LoyaltyEventProcessResponse out = service.process("t1", req);
        assertThat(out.isSuccess()).isTrue();
        assertThat(out.getVoucherIssuance()).isNotNull();
        assertThat(out.getVoucherIssuance().getStatus()).isEqualTo("SUCCESS");
        assertThat(out.getVoucherIssuance().getCode()).isEqualTo("MD-100-A01");
        assertThat(out.getNewBalance()).isEqualByComparingTo("950");
    }

    @Test
    void process_voucherFailureDoesNotBreakPointsSuccess() {
        when(campaignProperties.isResolutionLogEnabled()).thenReturn(false);
        when(programmeRuleContextLoader.load(eq("t1"), eq("default"), any()))
            .thenReturn(ProgrammeEvaluationContext.builder().resolvedTierMultiplier(BigDecimal.ONE).build());
        when(programmeService.getActiveConfigOrNull(eq("t1"), eq("default"))).thenReturn(null);

        RuleEvaluationResponse ruleEval = new RuleEvaluationResponse();
        ruleEval.setSuccess(true);
        ruleEval.setFinalPointsAwarded(BigDecimal.ZERO);
        RuleEvaluationResponse.MatchedRuleInfo m = new RuleEvaluationResponse.MatchedRuleInfo();
        m.setRuleUid("r1");
        m.setPriority(1);
        ruleEval.setMatchedRules(List.of(m));

        RuleEvaluationResponse.CatalogGrantInfo g = new RuleEvaluationResponse.CatalogGrantInfo();
        g.setSourceRuleUid("r1");
        g.setCatalogRewardUid("zenzek_voucher");
        g.setIssueMode("AUTO_ISSUE_ON_EVENT");
        g.setSelectionMode("BY_POINTS");
        g.setPointsToRedeem(new BigDecimal("200"));
        g.setValid(true);
        ruleEval.setCatalogGrants(List.of(g));
        when(ruleEvaluationService.evaluate(eq("t1"), any(RuleEvaluateRequest.class))).thenReturn(ruleEval);

        when(rewardCommandBuilder.build(any(), any(), any(), any(), any())).thenReturn(List.<CampaignBuiltAward>of());

        RewardIssueResponse issue = new RewardIssueResponse();
        issue.setMessage("ok");
        issue.setIdempotentReplay(false);
        issue.setNewBalance(new BigDecimal("1000"));
        when(rewardIssuanceService.issue(eq("t1"), any())).thenReturn(issue);

        when(voucherAutoIssueService.isEnabled()).thenReturn(true);
        VoucherIssueResponse failed = new VoucherIssueResponse();
        failed.setStatus("INSUFFICIENT_BALANCE");
        failed.setErrorMessage("Insufficient balance");
        when(voucherAutoIssueService.autoIssue(any(), any(), any(), any(), any(), any(), any())).thenReturn(failed);

        LoyaltyEventProcessRequest req = new LoyaltyEventProcessRequest();
        req.setProgrammeUid("default");
        req.setEvaluationScope("PROGRAMME_RULES");
        req.setCustomerId("cust_1");
        req.setEventType("BILLPAY");
        req.setTransactionId("evt_2");
        req.setAmount(new BigDecimal("100"));

        LoyaltyEventProcessResponse out = service.process("t1", req);
        assertThat(out.isSuccess()).isTrue();
        assertThat(out.getVoucherIssuance()).isNotNull();
        assertThat(out.getVoucherIssuance().getStatus()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(out.getNewBalance()).isEqualByComparingTo("1000");
    }
}
