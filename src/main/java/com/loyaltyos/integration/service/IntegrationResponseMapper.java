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
        EventProcessingResponse response = new EventProcessingResponse();
        response.setStatus("SUCCESS");
        response.setEventId(request.eventId());
        response.setTimestamp(resolveTimestamp(request));
        response.setIdempotencyKey(request.eventId());
        response.setProcessingTimeMs(processingTimeMs);

        EventProcessingResponse.CustomerInfo customer = new EventProcessingResponse.CustomerInfo();
        customer.setCustomerId(request.customerId());
        customer.setTierBefore(request.customerTierUid());
        customer.setTierAfter(request.customerTierUid());
        customer.setTierChanged(false);
        response.setCustomer(customer);

        EventProcessingResponse.EarningsInfo earnings = new EventProcessingResponse.EarningsInfo();
        BigDecimal rulePoints = core.getRulePointsAwarded() != null ? core.getRulePointsAwarded() : BigDecimal.ZERO;
        BigDecimal total = core.getTotalPointsAwarded() != null ? core.getTotalPointsAwarded() : BigDecimal.ZERO;
        BigDecimal previous = core.getNewBalance() != null && total != null
            ? core.getNewBalance().subtract(total)
            : BigDecimal.ZERO;
        earnings.setBasePoints(ruleEval != null && ruleEval.getBasePointsCalculated() != null
            ? ruleEval.getBasePointsCalculated() : rulePoints);
        earnings.setTierMultiplier(ruleEval != null && ruleEval.getTierMultiplier() != null
            ? ruleEval.getTierMultiplier() : BigDecimal.ONE);
        earnings.setFinalPoints(total);
        earnings.setPreviousBalance(previous);
        earnings.setNewBalance(core.getNewBalance());
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
            for (RuleEvaluationResponse.MatchedRuleInfo m : ruleEval.getMatchedRules()) {
                EventProcessingResponse.MatchedRuleLine line = new EventProcessingResponse.MatchedRuleLine();
                line.setRuleId(m.getRuleUid());
                line.setRuleName(m.getRuleName());
                line.setPriority(m.getPriority());
                line.setPointsAwarded(m.getPointsFromThisRule());
                rules.getMatched().add(line);
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
        return response;
    }

    public List<EventProcessingResponse.MatchedRuleLine> toMatchedRuleLines(RuleEvaluationResponse ruleEval) {
        List<EventProcessingResponse.MatchedRuleLine> lines = new ArrayList<>();
        if (ruleEval == null || ruleEval.getMatchedRules() == null) {
            return lines;
        }
        for (RuleEvaluationResponse.MatchedRuleInfo m : ruleEval.getMatchedRules()) {
            EventProcessingResponse.MatchedRuleLine line = new EventProcessingResponse.MatchedRuleLine();
            line.setRuleId(m.getRuleUid());
            line.setRuleName(m.getRuleName());
            line.setPriority(m.getPriority());
            line.setPointsAwarded(m.getPointsFromThisRule());
            lines.add(line);
        }
        return lines;
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
