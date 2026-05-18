package com.loyaltyos.campaigns.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.model.CampaignAwardType;
import com.loyaltyos.campaigns.model.CampaignBuiltAward;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignRewardCommandBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CampaignRewardCommandBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CampaignRewardCommandBuilder(new CampaignJsonSupport(MAPPER));
    }

    @Test
    void build_pointsBonus_createsIssueCommand() {
        Campaign campaign = campaignWithOffer(
            CampaignAwardType.POINTS_BONUS,
            "bonusPoints", new BigDecimal("25"),
            "expiryDays", 30
        );

        List<CampaignBuiltAward> awards = builder.build(
            List.of(campaign),
            event(),
            "txn-1",
            BigDecimal.ZERO,
            Instant.parse("2030-01-01T00:00:00Z")
        );

        assertThat(awards).hasSize(1);
        CampaignBuiltAward award = awards.getFirst();
        assertThat(award.pointsAwarded()).isEqualByComparingTo("25");
        assertThat(award.cashbackAmount()).isEqualByComparingTo("0");
        assertThat(award.issueCommand()).isNotNull();
        assertThat(award.issueCommand().getSourceCampaignUid()).isEqualTo("camp-1");
        assertThat(award.issueCommand().getIdempotencyKey())
            .isEqualTo("camp:camp-1:txn-1:POINTS_BONUS");
        assertThat(award.issueCommand().getExpiresAt()).isNotNull();
    }

    @Test
    void build_multiplierOnRulePoints_usesFinalRulePoints() {
        Campaign campaign = campaignWithOffer(
            CampaignAwardType.MULTIPLIER_ON_RULE_POINTS,
            "multiplierOnRulePoints", new BigDecimal("2.0")
        );

        List<CampaignBuiltAward> awards = builder.build(
            List.of(campaign),
            event(),
            "txn-2",
            new BigDecimal("100"),
            null
        );

        assertThat(awards.getFirst().pointsAwarded()).isEqualByComparingTo("100");
    }

    @Test
    void build_flatCashback_noIssueCommand() {
        Campaign campaign = campaignWithOffer(
            CampaignAwardType.FLAT_CASHBACK,
            "cashbackValue", new BigDecimal("15")
        );

        List<CampaignBuiltAward> awards = builder.build(
            List.of(campaign),
            event(),
            "txn-3",
            BigDecimal.ZERO,
            null
        );

        assertThat(awards.getFirst().cashbackAmount()).isEqualByComparingTo("15");
        assertThat(awards.getFirst().issueCommand()).isNull();
    }

    @Test
    void build_percentCashback_computesFromAmount() {
        Campaign campaign = campaignWithOffer(
            CampaignAwardType.PERCENT_CASHBACK,
            "cashbackValue", new BigDecimal("10")
        );

        List<CampaignBuiltAward> awards = builder.build(
            List.of(campaign),
            event(),
            "txn-4",
            BigDecimal.ZERO,
            null
        );

        assertThat(awards.getFirst().cashbackAmount()).isEqualByComparingTo("50");
    }

    private static CampaignEventContext event() {
        return new CampaignEventContext("cust-1", "gold", "PURCHASE", new BigDecimal("500"), "WEB", "IN");
    }

    private static Campaign campaignWithOffer(String awardType, Object... keyValues) {
        Campaign c = new Campaign();
        c.setCampaignUid("camp-1");
        c.setName("Test Campaign");
        ObjectNode offer = MAPPER.createObjectNode();
        offer.put("awardType", awardType);
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object val = keyValues[i + 1];
            if (val instanceof BigDecimal bd) {
                offer.put(key, bd);
            } else if (val instanceof Integer in) {
                offer.put(key, in);
            }
        }
        c.setOfferConfig(offer);
        return c;
    }
}
