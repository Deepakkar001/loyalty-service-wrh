package com.loyaltyos.referrals.model;

/**
 * Tenant-authored milestone rule stored in programme config. Stages reference {@link #key}.
 */
public class ReferralMilestoneRule {

    private String key;
    private String label;
    private String description;
    private boolean enabled = true;
    private ReferralRuleTrigger trigger = ReferralRuleTrigger.LINK;
    private ReferralRuleCriteria criteria = new ReferralRuleCriteria();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ReferralRuleTrigger getTrigger() {
        return trigger != null ? trigger : ReferralRuleTrigger.LINK;
    }

    public void setTrigger(ReferralRuleTrigger trigger) {
        this.trigger = trigger;
    }

    public ReferralRuleCriteria getCriteria() {
        return criteria != null ? criteria : new ReferralRuleCriteria();
    }

    public void setCriteria(ReferralRuleCriteria criteria) {
        this.criteria = criteria;
    }
}
