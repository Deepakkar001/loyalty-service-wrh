package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.dto.RuleSandboxStatusResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignRuleSandboxPass;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignRuleSandboxPassRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetCustomerRepository;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.enums.RuleType;
import com.loyaltyos.rules.repository.EarnRuleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignRuleSandboxService {

    private final CampaignRuleSandboxPassRepository sandboxPassRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignTargetCustomerRepository targetCustomerRepository;
    private final EarnRuleRepository earnRuleRepository;

    public CampaignRuleSandboxService(
        CampaignRuleSandboxPassRepository sandboxPassRepository,
        CampaignRepository campaignRepository,
        CampaignTargetCustomerRepository targetCustomerRepository,
        EarnRuleRepository earnRuleRepository
    ) {
        this.sandboxPassRepository = Objects.requireNonNull(sandboxPassRepository, "sandboxPassRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.targetCustomerRepository = Objects.requireNonNull(targetCustomerRepository, "targetCustomerRepository");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
    }

    @Transactional(readOnly = true)
    public RuleSandboxStatusResponse getSandboxStatus(String tenantId, String ruleUid) {
        Optional<CampaignRuleSandboxPass> pass = sandboxPassRepository.findByTenantIdAndRuleUid(tenantId, ruleUid);
        if (pass.isEmpty()) {
            return RuleSandboxStatusResponse.notPassed(tenantId, ruleUid);
        }
        CampaignRuleSandboxPass row = pass.get();
        return RuleSandboxStatusResponse.passed(
            tenantId,
            ruleUid,
            row.getCampaignUid(),
            row.getCustomerId(),
            row.isTargetedCheckOk(),
            row.getPassedAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasSandboxPass(String tenantId, String ruleUid) {
        return sandboxPassRepository.existsByTenantIdAndRuleUid(tenantId, ruleUid);
    }

    /**
     * Validates TARGETED audience membership for sandbox. Returns failure code when not in list.
     */
    @Transactional(readOnly = true)
    public TargetedCustomerCheckResult checkTargetedCustomer(String tenantId, Campaign campaign, String customerId) {
        CustomerScope scope = campaign.getCustomerScope();
        if (scope == null || scope == CustomerScope.ALL) {
            return TargetedCustomerCheckResult.success();
        }
        if (customerId == null || customerId.isBlank()) {
            return TargetedCustomerCheckResult.fail("TARGETED_CUSTOMER_NOT_IN_LIST", "customerId is required for targeted campaigns");
        }
        boolean inList = targetCustomerRepository.existsByTenantIdAndCampaignUidAndCustomerId(
            tenantId,
            campaign.getCampaignUid(),
            customerId.trim()
        );
        if (!inList) {
            return TargetedCustomerCheckResult.fail(
                "TARGETED_CUSTOMER_NOT_IN_LIST",
                "customerId is not in the campaign target list"
            );
        }
        return TargetedCustomerCheckResult.success();
    }

    @Transactional
    public void recordPass(
        String tenantId,
        String campaignUid,
        String ruleUid,
        String customerId,
        Map<String, Object> payload,
        boolean targetedCheckOk,
        String passedBy
    ) {
        String hash = sha256Hex(stablePayloadJson(payload));
        Instant now = Instant.now();
        CampaignRuleSandboxPass row = sandboxPassRepository.findByTenantIdAndRuleUid(tenantId, ruleUid)
            .orElseGet(CampaignRuleSandboxPass::new);
        row.setTenantId(tenantId);
        row.setCampaignUid(campaignUid);
        row.setRuleUid(ruleUid);
        row.setCustomerId(customerId != null ? customerId.trim() : "");
        row.setPayloadHash(hash);
        row.setTargetedCheckOk(targetedCheckOk);
        row.setPassedBy(passedBy);
        row.setPassedAt(now);
        sandboxPassRepository.save(row);
    }

    @Transactional(readOnly = true)
    public ResolvedCampaignRule resolveCampaignRule(String tenantId, String ruleUid, String requestCampaignUid) {
        EarnRule rule = earnRuleRepository.loadForAdminEditByRuleUid(tenantId, ruleUid)
            .orElse(null);
        if (rule == null || rule.getRuleType() != RuleType.CAMPAIGN) {
            return null;
        }
        String campaignUid = rule.getCampaignUid();
        if (campaignUid == null || campaignUid.isBlank()) {
            return null;
        }
        if (requestCampaignUid != null && !requestCampaignUid.isBlank()
            && !campaignUid.equals(requestCampaignUid.trim())) {
            return null;
        }
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));
        return new ResolvedCampaignRule(rule, campaign);
    }

    public record ResolvedCampaignRule(EarnRule rule, Campaign campaign) {}

    public record TargetedCustomerCheckResult(boolean passed, String errorCode, String errorMessage) {
        public static TargetedCustomerCheckResult success() {
            return new TargetedCustomerCheckResult(true, null, null);
        }

        public static TargetedCustomerCheckResult fail(String code, String message) {
            return new TargetedCustomerCheckResult(false, code, message);
        }
    }

    private static String stablePayloadJson(Map<String, Object> payload) {
        if (payload == null) {
            return "{}";
        }
        return payload.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
