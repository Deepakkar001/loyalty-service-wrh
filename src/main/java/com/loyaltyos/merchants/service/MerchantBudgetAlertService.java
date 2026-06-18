package com.loyaltyos.merchants.service;

import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.dto.MerchantBudgetAlertResponse;
import com.loyaltyos.merchants.entity.MerchantBudgetAlert;
import com.loyaltyos.merchants.repository.MerchantBudgetAlertRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.onboarding.config.AppUrlConfig;
import com.loyaltyos.onboarding.service.PlatformEmailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantBudgetAlertService {

    private static final Logger log = LoggerFactory.getLogger(MerchantBudgetAlertService.class);

    private final MerchantBudgetAlertRepository alertRepository;
    private final MerchantRepository merchantRepository;
    private final CampaignRepository campaignRepository;
    private final MerchantProperties merchantProperties;
    private final AppUrlConfig appUrlConfig;
    private final PlatformEmailService emailService;

    public MerchantBudgetAlertService(
        MerchantBudgetAlertRepository alertRepository,
        MerchantRepository merchantRepository,
        CampaignRepository campaignRepository,
        MerchantProperties merchantProperties,
        AppUrlConfig appUrlConfig,
        PlatformEmailService emailService
    ) {
        this.alertRepository = Objects.requireNonNull(alertRepository, "alertRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
        this.emailService = Objects.requireNonNull(emailService, "emailService");
    }

    @Transactional
    public void tryNotifyMerchantBudgetAlert(
        String tenantId,
        String campaignUid,
        BigDecimal consumed,
        BigDecimal total,
        BigDecimal thresholdPct
    ) {
        if (thresholdPct == null) {
            thresholdPct = merchantProperties.getDefaultBudgetAlertPct();
        }
        var campaignOpt = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid);
        if (campaignOpt.isEmpty()) {
            return;
        }
        var campaign = campaignOpt.get();
        String merchantUid = campaign.getMerchantId();
        if (merchantUid == null || merchantUid.isBlank()) {
            return;
        }

        MerchantBudgetAlert row = new MerchantBudgetAlert();
        row.setTenantId(tenantId);
        row.setMerchantUid(merchantUid);
        row.setCampaignUid(campaignUid);
        row.setAlertThresholdPct(thresholdPct);
        row.setBudgetConsumed(consumed);
        row.setBudgetTotal(total);
        try {
            alertRepository.saveAndFlush(row);
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }

        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid).orElse(null);
        if (merchant == null || merchant.getContactEmail() == null || merchant.getContactEmail().isBlank()) {
            log.warn("Merchant budget alert: no contact email tenant={} merchant={}", tenantId, merchantUid);
            return;
        }

        BigDecimal pct = consumed.multiply(new BigDecimal("100"))
            .divide(total, 1, RoundingMode.HALF_UP);
        String campaignsUrl = appUrlConfig.getPortalUrl() + "/merchant/campaigns";
        String body = """
Your campaign "%s" has reached %s%% of its allocated budget.

Budget consumed: %s
Budget total: %s

Review your campaigns here: %s
""".formatted(
            campaign.getName(),
            pct,
            consumed,
            total,
            campaignsUrl
        );

        boolean sent = emailService.sendPlainText(
            merchant.getContactEmail(),
            "Campaign budget alert — " + campaign.getName(),
            body,
            "Campaigns URL: " + campaignsUrl
        );
        if (!sent) {
            log.info("Merchant budget alert logged (email skipped) tenant={} campaign={}", tenantId, campaignUid);
        }
    }

    @Transactional(readOnly = true)
    public List<MerchantBudgetAlertResponse> listAlertsForMerchant(String tenantId, String merchantUid) {
        return alertRepository.findByTenantIdAndMerchantUidOrderByNotifiedAtDesc(tenantId, merchantUid)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private MerchantBudgetAlertResponse toResponse(MerchantBudgetAlert alert) {
        MerchantBudgetAlertResponse response = new MerchantBudgetAlertResponse();
        response.setCampaignUid(alert.getCampaignUid());
        response.setAlertThresholdPct(alert.getAlertThresholdPct());
        response.setBudgetConsumed(alert.getBudgetConsumed());
        response.setBudgetTotal(alert.getBudgetTotal());
        response.setNotifiedAt(alert.getNotifiedAt());
        return response;
    }
}
