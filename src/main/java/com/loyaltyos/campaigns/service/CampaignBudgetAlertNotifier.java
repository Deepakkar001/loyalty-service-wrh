package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.onboarding.domain.entity.WebhookSubscription;
import com.loyaltyos.onboarding.domain.enums.WebhookVerificationStatus;
import com.loyaltyos.onboarding.repository.WebhookSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CampaignBudgetAlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(CampaignBudgetAlertNotifier.class);

    public static final String EVENT_BUDGET_ALERT = "campaign.budget.alert";
    public static final String EVENT_BUDGET_EXHAUSTED = "campaign.budget.exhausted";

    private final CampaignProperties campaignProperties;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public CampaignBudgetAlertNotifier(
        CampaignProperties campaignProperties,
        WebhookSubscriptionRepository webhookSubscriptionRepository,
        ObjectMapper objectMapper,
        RestTemplateBuilder restTemplateBuilder
    ) {
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.webhookSubscriptionRepository = Objects.requireNonNull(webhookSubscriptionRepository, "webhookSubscriptionRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.restTemplate = restTemplateBuilder.build();
    }

    public void notifyBudgetAlert(
        String tenantId,
        String programmeUid,
        String campaignUid,
        BigDecimal budgetConsumed,
        BigDecimal budgetTotal,
        BigDecimal alertThresholdPct
    ) {
        ObjectNode payload = basePayload(tenantId, programmeUid, campaignUid, EVENT_BUDGET_ALERT);
        payload.put("budgetConsumed", budgetConsumed);
        payload.put("budgetTotal", budgetTotal);
        if (alertThresholdPct != null) {
            payload.put("alertThresholdPct", alertThresholdPct);
        }
        dispatch(tenantId, payload);
    }

    public void notifyBudgetExhausted(String tenantId, String programmeUid, String campaignUid) {
        ObjectNode payload = basePayload(tenantId, programmeUid, campaignUid, EVENT_BUDGET_EXHAUSTED);
        dispatch(tenantId, payload);
    }

    private ObjectNode basePayload(String tenantId, String programmeUid, String campaignUid, String eventType) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", eventType);
        payload.put("tenantId", tenantId);
        payload.put("programmeUid", programmeUid);
        payload.put("campaignUid", campaignUid);
        payload.put("occurredAt", Instant.now().toString());
        return payload;
    }

    private void dispatch(String tenantId, ObjectNode payload) {
        log.info(
            "Campaign alert (Kafka topic {}): {}",
            campaignProperties.getBudgetAlertKafkaTopic(),
            payload
        );

        if (!campaignProperties.isBudgetAlertWebhookEnabled()) {
            return;
        }

        webhookSubscriptionRepository.findFirstByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId)
            .filter(this::isDeliverable)
            .ifPresent(sub -> postWebhook(sub, payload));
    }

    private boolean isDeliverable(WebhookSubscription sub) {
        return Boolean.TRUE.equals(sub.getActive())
            && sub.getVerificationStatus() == WebhookVerificationStatus.VERIFIED
            && sub.getEndpointUrl() != null
            && !sub.getEndpointUrl().isBlank();
    }

    private void postWebhook(WebhookSubscription sub, ObjectNode payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            restTemplate.postForEntity(sub.getEndpointUrl(), entity, String.class);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn(
                "Campaign alert webhook delivery failed tenant={} url={}: {}",
                sub.getTenantId(),
                sub.getEndpointUrl(),
                e.getMessage()
            );
        }
    }
}
