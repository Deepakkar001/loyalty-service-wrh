package com.loyaltyos.campaigns.model;

/**
 * Parsed {@code evaluationScope} from event requests. Default (blank) runs campaigns + programme rules only.
 */
public record EvaluationScopeFlags(boolean runCampaigns, boolean runProgrammeRules, boolean runReferral) {

    public static EvaluationScopeFlags defaults() {
        return new EvaluationScopeFlags(true, true, false);
    }

    public static EvaluationScopeFlags parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaults();
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        if ("BOTH".equals(normalized)) {
            return new EvaluationScopeFlags(true, true, false);
        }
        if ("CAMPAIGN".equals(normalized) || "CAMPAIGNS".equals(normalized)) {
            return new EvaluationScopeFlags(true, false, false);
        }
        if ("PROGRAMME_RULES".equals(normalized) || "RULES".equals(normalized) || "PROGRAMME".equals(normalized)) {
            return new EvaluationScopeFlags(false, true, false);
        }
        if ("REFERRAL".equals(normalized) || "REFERRALS".equals(normalized)) {
            return new EvaluationScopeFlags(false, false, true);
        }

        boolean campaigns = false;
        boolean rules = false;
        boolean referral = false;
        for (String part : normalized.split("[,;\\s]+")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            switch (token) {
                case "BOTH" -> {
                    campaigns = true;
                    rules = true;
                }
                case "CAMPAIGN", "CAMPAIGNS" -> campaigns = true;
                case "PROGRAMME_RULES", "RULES", "PROGRAMME" -> rules = true;
                case "REFERRAL", "REFERRALS" -> referral = true;
                default -> {
                    // ignore unknown tokens for forward compatibility
                }
            }
        }
        if (!campaigns && !rules && !referral) {
            return defaults();
        }
        return new EvaluationScopeFlags(campaigns, rules, referral);
    }
}
