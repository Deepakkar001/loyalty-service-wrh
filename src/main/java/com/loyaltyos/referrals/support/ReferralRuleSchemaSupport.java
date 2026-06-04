package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.dto.ReferralRuleSchemaResponse;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReferralRuleSchemaSupport {

    private ReferralRuleSchemaSupport() {}

    public static ReferralRuleSchemaResponse build() {
        ReferralRuleSchemaResponse schema = new ReferralRuleSchemaResponse();

        List<ReferralRuleSchemaResponse.TriggerOption> triggers = new ArrayList<>();
        triggers.add(trigger(
            ReferralRuleTrigger.LINK.name(),
            "When referee links code",
            "Runs once at successful referral link (integration link API)."
        ));
        triggers.add(trigger(
            ReferralRuleTrigger.PURCHASE.name(),
            "On purchase event",
            "Evaluated when a PURCHASE event is processed for the referee."
        ));
        triggers.add(trigger(
            ReferralRuleTrigger.INTEGRATION_EVENT.name(),
            "On integration event",
            "Evaluated when event type matches configured types (e.g. PROFILE_COMPLETED)."
        ));
        schema.setTriggers(triggers);

        List<ReferralRuleSchemaResponse.CriteriaField> fields = new ArrayList<>();
        fields.add(field("eventTypes", "Integration event types", "eventTypes", "Comma-separated, e.g. PROFILE_COMPLETED", false));
        fields.add(field("minPurchaseCount", "Minimum purchases", "number", "e.g. 3 for third purchase", true));
        fields.add(field("firstPurchaseOnly", "First purchase only", "boolean", "Reward only on the first qualifying purchase", true));
        fields.add(field("minSpend", "Minimum spend", "decimal", "Total spend in scope", true));
        fields.add(field("windowDays", "Time window (days)", "number", "Only count purchases in the last N days", true));
        fields.add(field("merchantId", "Merchant ID", "text", "Optional filter", true));
        fields.add(field("category", "Category", "text", "Optional filter", true));
        fields.add(field("channel", "Channel", "text", "Optional filter (purchase or event metadata)", true));
        fields.add(field("sku", "SKU", "text", "Optional product SKU filter", true));
        fields.add(field("region", "Region / geo", "text", "Country, state, or region code", true));
        schema.setCriteriaFields(fields);

        schema.setTemplates(templates());
        return schema;
    }

    public static ReferralRuleSchemaResponse buildForProgramme(
        String programmeUid,
        ReferralProgrammeEventSchemaCatalog catalog
    ) {
        ReferralRuleSchemaResponse schema = build();
        schema.setProgrammeUid(programmeUid);
        if (catalog != null) {
            schema.setProgrammeEventTypes(new ArrayList<>(catalog.getEventTypes()));
            schema.setCriteriaFieldsByEvent(catalog.toCriteriaFieldsByEvent());
        }
        return schema;
    }

    private static ReferralRuleSchemaResponse.TriggerOption trigger(String value, String label, String description) {
        ReferralRuleSchemaResponse.TriggerOption t = new ReferralRuleSchemaResponse.TriggerOption();
        t.setValue(value);
        t.setLabel(label);
        t.setDescription(description);
        return t;
    }

    private static ReferralRuleSchemaResponse.CriteriaField field(
        String key,
        String label,
        String type,
        String hint,
        boolean purchaseOnly
    ) {
        ReferralRuleSchemaResponse.CriteriaField f = new ReferralRuleSchemaResponse.CriteriaField();
        f.setKey(key);
        f.setLabel(label);
        f.setType(type);
        f.setHint(hint);
        f.setPurchaseOnly(purchaseOnly);
        return f;
    }

    private static List<ReferralRuleSchemaResponse.RuleTemplate> templates() {
        List<ReferralRuleSchemaResponse.RuleTemplate> list = new ArrayList<>();
        list.add(template(
            "signup_reward",
            "Sign-up reward",
            "Reward when referral link completes",
            ReferralRuleTrigger.LINK.name(),
            Map.of()
        ));
        list.add(template(
            "profile_completed",
            "Profile completed",
            "Reward when profile completion event is received",
            ReferralRuleTrigger.INTEGRATION_EVENT.name(),
            Map.of("eventTypes", List.of("PROFILE_COMPLETED"))
        ));
        list.add(template(
            "first_purchase",
            "First purchase",
            "Reward on referee's first purchase",
            ReferralRuleTrigger.PURCHASE.name(),
            Map.of("minPurchaseCount", 1, "firstPurchaseOnly", true)
        ));
        list.add(template(
            "nth_purchase",
            "Nth purchase",
            "Reward after N purchases (set count below)",
            ReferralRuleTrigger.PURCHASE.name(),
            Map.of("minPurchaseCount", 2)
        ));
        list.add(template(
            "spend_threshold",
            "Spend threshold",
            "Reward when spend reaches minimum",
            ReferralRuleTrigger.PURCHASE.name(),
            Map.of("minSpend", 100, "minPurchaseCount", 1)
        ));
        return list;
    }

    private static ReferralRuleSchemaResponse.RuleTemplate template(
        String key,
        String label,
        String description,
        String trigger,
        Map<String, Object> criteria
    ) {
        ReferralRuleSchemaResponse.RuleTemplate t = new ReferralRuleSchemaResponse.RuleTemplate();
        t.setKey(key);
        t.setLabel(label);
        t.setDescription(description);
        t.setTrigger(trigger);
        t.setCriteriaDefaults(criteria);
        return t;
    }
}
