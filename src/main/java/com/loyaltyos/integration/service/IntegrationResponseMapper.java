package com.loyaltyos.integration.service;

import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse.AppliedCampaignLine;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class IntegrationResponseMapper {

    public EventProcessingResponse toSuccessResponse(
        IntegrationParsedEvent request,
        LoyaltyEventProcessResponse core,
        RuleEvaluationResponse ruleEval,
        int processingTimeMs
    ) {
        if (ruleEval == null) {
            ruleEval = core.getRuleEvaluation();
        }

        EventProcessingResponse response = new EventProcessingResponse();
        response.setStatus("SUCCESS");
        response.setEventId(request.eventId());
        response.setTimestamp(resolveTimestamp(request));
        response.setIdempotencyKey(request.eventId());
        response.setProcessingTimeMs(processingTimeMs);

        EventProcessingResponse.CustomerInfo customer = new EventProcessingResponse.CustomerInfo();
        customer.setCustomerId(request.customerId());
        if (core.getTierBeforeName() != null || core.getTierAfterName() != null) {
            customer.setTierBefore(core.getTierBeforeName());
            customer.setTierAfter(core.getTierAfterName());
            customer.setTierChanged(core.isTierChanged());
        } else if (core.getTierBeforeUid() != null || core.getTierAfterUid() != null) {
            customer.setTierBefore(core.getTierBeforeUid());
            customer.setTierAfter(core.getTierAfterUid());
            customer.setTierChanged(core.isTierChanged());
        } else {
            customer.setTierBefore(request.customerTierUid());
            customer.setTierAfter(request.customerTierUid());
            customer.setTierChanged(false);
        }
        response.setCustomer(customer);

        EventProcessingResponse.EarningsInfo earnings = new EventProcessingResponse.EarningsInfo();
        BigDecimal rulePoints = core.getRulePointsAwarded() != null ? core.getRulePointsAwarded() : BigDecimal.ZERO;
        BigDecimal total = core.getTotalPointsAwarded() != null ? core.getTotalPointsAwarded() : BigDecimal.ZERO;
        BigDecimal previous = core.getPreviousBalance();
        if (previous == null && core.getNewBalance() != null && total != null) {
            previous = core.getNewBalance().subtract(total);
        }
        if (previous == null) {
            previous = BigDecimal.ZERO;
        }
        earnings.setBasePoints(ruleEval != null && ruleEval.getBasePointsCalculated() != null
            ? ruleEval.getBasePointsCalculated() : rulePoints);
        earnings.setTierMultiplier(ruleEval != null && ruleEval.getTierMultiplier() != null
            ? ruleEval.getTierMultiplier() : BigDecimal.ONE);
        earnings.setFinalPoints(total);
        // If a voucher redemption succeeded in a separate transaction, show balances around the redemption.
        if (core.getVoucherIssuance() != null
            && "SUCCESS".equalsIgnoreCase(core.getVoucherIssuance().getStatus())
            && core.getVoucherIssuance().getPointsRedeemed() != null
            && core.getVoucherIssuance().getPointsRedeemed().signum() > 0
            && core.getNewBalance() != null) {
            BigDecimal redeemed = core.getVoucherIssuance().getPointsRedeemed();
            earnings.setNewBalance(core.getNewBalance());
            earnings.setPreviousBalance(core.getNewBalance().add(redeemed));
        } else {
            earnings.setPreviousBalance(previous);
            earnings.setNewBalance(core.getNewBalance());
        }
        if (core.getReferralPointsToOtherCustomers() != null
            && core.getReferralPointsToOtherCustomers().signum() > 0) {
            earnings.setReferralPointsToOtherCustomers(core.getReferralPointsToOtherCustomers());
        }
        response.setEarnings(earnings);

        EventProcessingResponse.RulesInfo rules = new EventProcessingResponse.RulesInfo();
        if ((ruleEval == null || ruleEval.getMatchedRules() == null || ruleEval.getMatchedRules().isEmpty())
            && rulePoints.compareTo(BigDecimal.ZERO) > 0) {
            EventProcessingResponse.MatchedRuleLine synthetic = new EventProcessingResponse.MatchedRuleLine();
            synthetic.setRuleId("rule_engine");
            synthetic.setRuleName("Loyalty rules applied");
            synthetic.setPointsAwarded(rulePoints);
            rules.getMatched().add(synthetic);
        }
        if (ruleEval != null && ruleEval.getMatchedRules() != null) {
            BigDecimal tierMultiplier = ruleEval.getTierMultiplier();
            for (RuleEvaluationResponse.MatchedRuleInfo m : ruleEval.getMatchedRules()) {
                rules.getMatched().add(toMatchedRuleLine(m, tierMultiplier));
            }
        }
        if (ruleEval != null && ruleEval.getSuppressedRules() != null) {
            for (RuleEvaluationResponse.SuppressedRuleInfo s : ruleEval.getSuppressedRules()) {
                EventProcessingResponse.SuppressedRuleLine line = new EventProcessingResponse.SuppressedRuleLine();
                line.setRuleId(s.getRuleUid());
                line.setRuleName(s.getRuleUid());
                line.setReason(s.getReason());
                rules.getSuppressed().add(line);
            }
        }
        response.setRules(rules);

        EventProcessingResponse.CampaignsInfo campaigns = new EventProcessingResponse.CampaignsInfo();
        if (core.getCampaignsApplied() != null) {
            for (AppliedCampaignLine c : core.getCampaignsApplied()) {
                EventProcessingResponse.EligibleCampaignLine line = new EventProcessingResponse.EligibleCampaignLine();
                line.setCampaignId(c.getCampaignUid());
                line.setCampaignName(c.getCampaignName());
                line.setBonusPoints(c.getPointsAwarded());
                campaigns.getEligible().add(line);
            }
        }
        response.setCampaigns(campaigns);

        if (core.getVoucherIssuance() != null) {
            LoyaltyEventProcessResponse.VoucherIssuanceResult vi = core.getVoucherIssuance();
            EventProcessingResponse.VoucherIssuanceInfo out = new EventProcessingResponse.VoucherIssuanceInfo();
            out.setRuleUid(vi.getRuleUid());
            out.setCatalogRewardUid(vi.getCatalogRewardUid());
            out.setStatus(vi.getStatus());
            out.setErrorMessage(vi.getErrorMessage());
            out.setPointsRedeemed(vi.getPointsRedeemed());
            out.setSelectedFaceValue(vi.getSelectedFaceValue());
            out.setSelectedCurrency(vi.getSelectedCurrency());
            out.setCode(vi.getCode());
            out.setPin(vi.getPin());
            response.setVoucherIssuance(out);
        }

        return response;
    }

    public List<EventProcessingResponse.MatchedRuleLine> toMatchedRuleLines(RuleEvaluationResponse ruleEval) {
        List<EventProcessingResponse.MatchedRuleLine> lines = new ArrayList<>();
        if (ruleEval == null || ruleEval.getMatchedRules() == null) {
            return lines;
        }
        BigDecimal tierMultiplier = ruleEval.getTierMultiplier();
        for (RuleEvaluationResponse.MatchedRuleInfo m : ruleEval.getMatchedRules()) {
            lines.add(toMatchedRuleLine(m, tierMultiplier));
        }
        return lines;
    }

    private static EventProcessingResponse.MatchedRuleLine toMatchedRuleLine(
        RuleEvaluationResponse.MatchedRuleInfo matchedRule,
        BigDecimal tierMultiplier
    ) {
        EventProcessingResponse.MatchedRuleLine line = new EventProcessingResponse.MatchedRuleLine();
        line.setRuleId(matchedRule.getRuleUid());
        line.setRuleName(matchedRule.getRuleName());
        line.setPriority(matchedRule.getPriority());
        line.setPointsAwarded(matchedRule.getPointsFromThisRule());
        if (tierMultiplier != null) {
            line.setMultiplier(tierMultiplier);
        }
        return line;
    }

    private static Instant resolveTimestamp(IntegrationParsedEvent request) {
        Object ts = request.metadata().get("timestamp");
        if (ts == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(String.valueOf(ts));
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
