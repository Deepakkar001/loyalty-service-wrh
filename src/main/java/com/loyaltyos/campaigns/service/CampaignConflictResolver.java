package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.enums.StackMode;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignResolutionResult;
import com.loyaltyos.campaigns.model.DroppedCampaign;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CampaignConflictResolver {

    private final CampaignJsonSupport jsonSupport;

    public CampaignConflictResolver(CampaignJsonSupport jsonSupport) {
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "jsonSupport");
    }

    public CampaignResolutionResult resolve(List<Campaign> qualifyingCampaigns, CampaignEventContext event) {
        List<DroppedCampaign> dropped = new ArrayList<>();
        if (qualifyingCampaigns == null || qualifyingCampaigns.isEmpty()) {
            return new CampaignResolutionResult(List.of(), dropped, false, null);
        }

        Map<String, List<Campaign>> groups = new LinkedHashMap<>();
        for (Campaign c : qualifyingCampaigns) {
            String key = groupKey(c);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        List<Campaign> survivors = new ArrayList<>();
        String resolutionMode = null;

        for (List<Campaign> group : groups.values()) {
            group.sort(Comparator.comparing(Campaign::getPriority, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            Campaign authority = group.getFirst();
            StackMode mode = authority.getStackMode() == null ? StackMode.ADDITIVE : authority.getStackMode();
            if (resolutionMode == null) {
                resolutionMode = mode.name();
            }

            switch (mode) {
                case FIRST_MATCH -> {
                    survivors.add(authority);
                    for (int i = 1; i < group.size(); i++) {
                        dropped.add(drop(group.get(i), DropReason.LOWER_PRIORITY_IN_GROUP));
                    }
                }
                case BEST_OFFER -> {
                    Campaign winner = pickBestOffer(group, event);
                    survivors.add(winner);
                    for (Campaign c : group) {
                        if (!c.getCampaignUid().equals(winner.getCampaignUid())) {
                            dropped.add(drop(c, DropReason.LOWER_VALUE_IN_GROUP));
                        }
                    }
                }
                case ADDITIVE -> survivors.addAll(group);
                default -> survivors.addAll(group);
            }
        }

        return applyGlobalRewardCap(survivors, dropped, event, resolutionMode);
    }

    private CampaignResolutionResult applyGlobalRewardCap(
        List<Campaign> survivors,
        List<DroppedCampaign> dropped,
        CampaignEventContext event,
        String resolutionMode
    ) {
        BigDecimal total = BigDecimal.ZERO;
        List<BigDecimal> estimates = new ArrayList<>();
        for (Campaign c : survivors) {
            CampaignOfferConfig offer = jsonSupport.parseOfferConfig(c.getOfferConfig());
            BigDecimal est = CampaignRewardEstimateHelper.estimate(c, offer, event);
            estimates.add(est);
            total = total.add(est);
        }

        BigDecimal combinedCap = survivors.stream()
            .map(Campaign::getGlobalRewardCap)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);

        if (combinedCap == null || total.compareTo(combinedCap) <= 0) {
            return new CampaignResolutionResult(List.copyOf(survivors), dropped, false, resolutionMode);
        }

        BigDecimal factor = combinedCap.divide(total, 8, RoundingMode.HALF_UP);
        List<Campaign> clipped = new ArrayList<>();
        for (int i = 0; i < survivors.size(); i++) {
            Campaign c = survivors.get(i);
            BigDecimal est = estimates.get(i);
            BigDecimal scaled = est.multiply(factor).setScale(4, RoundingMode.HALF_UP);
            if (scaled.signum() <= 0) {
                dropped.add(drop(c, DropReason.GLOBAL_CAP_HIT));
            } else {
                clipped.add(c);
            }
        }

        return new CampaignResolutionResult(clipped, dropped, true, resolutionMode);
    }

    private Campaign pickBestOffer(List<Campaign> group, CampaignEventContext event) {
        Campaign best = group.getFirst();
        BigDecimal bestValue = estimate(best, event);
        for (int i = 1; i < group.size(); i++) {
            Campaign candidate = group.get(i);
            BigDecimal value = estimate(candidate, event);
            int cmp = value.compareTo(bestValue);
            if (cmp > 0) {
                best = candidate;
                bestValue = value;
            } else if (cmp == 0 && candidate.getPriority() > best.getPriority()) {
                best = candidate;
                bestValue = value;
            }
        }
        return best;
    }

    private BigDecimal estimate(Campaign campaign, CampaignEventContext event) {
        CampaignOfferConfig offer = jsonSupport.parseOfferConfig(campaign.getOfferConfig());
        return CampaignRewardEstimateHelper.estimate(campaign, offer, event);
    }

    private static String groupKey(Campaign campaign) {
        if (campaign.getMutualExclGroup() != null && !campaign.getMutualExclGroup().isBlank()) {
            return campaign.getMutualExclGroup().trim();
        }
        return "solo:" + campaign.getCampaignUid();
    }

    private static DroppedCampaign drop(Campaign campaign, DropReason reason) {
        return new DroppedCampaign(campaign.getCampaignUid(), campaign.getName(), reason);
    }
}
