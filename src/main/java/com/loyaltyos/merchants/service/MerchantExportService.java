package com.loyaltyos.merchants.service;

import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantExportService {

    private final MerchantRepository merchantRepository;

    public MerchantExportService(MerchantRepository merchantRepository) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
    }

    @Transactional(readOnly = true)
    public String exportCsv(String tenantId) {
        List<Merchant> merchants = merchantRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("merchantUid,legalName,displayName,category,contactEmail,onboardingStage,earnRateMultiplier,settlementCycle,active,suspended,settlementHold,createdAt\n");
        for (Merchant m : merchants) {
            sb.append(csv(m.getMerchantUid())).append(',');
            sb.append(csv(m.getLegalName())).append(',');
            sb.append(csv(m.getDisplayName())).append(',');
            sb.append(csv(m.getCategory())).append(',');
            sb.append(csv(m.getContactEmail())).append(',');
            sb.append(csv(m.getOnboardingStage() != null ? m.getOnboardingStage().name() : "")).append(',');
            sb.append(m.getEarnRateMultiplier() != null ? m.getEarnRateMultiplier() : "").append(',');
            sb.append(csv(m.getSettlementCycle() != null ? m.getSettlementCycle().name() : "")).append(',');
            sb.append(m.isActive()).append(',');
            sb.append(m.isSuspended()).append(',');
            sb.append(m.isSettlementHold()).append(',');
            sb.append(m.getCreatedAt() != null ? m.getCreatedAt() : "").append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
