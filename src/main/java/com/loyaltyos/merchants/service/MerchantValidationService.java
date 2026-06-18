package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantValidationService {

    private final MerchantProperties merchantProperties;
    private final ObjectMapper objectMapper;

    public MerchantValidationService(MerchantProperties merchantProperties, ObjectMapper objectMapper) {
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void requireContactEmailForActivation(Merchant merchant) {
        if (merchant.getContactEmail() == null || merchant.getContactEmail().isBlank()) {
            throw new InvalidStateException(
                "Contact email is required before activation",
                merchant.getOnboardingStage().name(),
                "ACTIVE"
            );
        }
    }

    public void validateEarnRateMultiplier(BigDecimal multiplier) {
        if (multiplier == null) {
            return;
        }
        BigDecimal min = merchantProperties.getMinEarnRateMultiplier();
        BigDecimal max = merchantProperties.getMaxEarnRateMultiplier();
        if (multiplier.compareTo(min) < 0 || multiplier.compareTo(max) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Earn rate multiplier must be between " + min + " and " + max
            );
        }
    }

    public void validateCommissionConfigJson(String commissionConfigJson) {
        if (commissionConfigJson == null || commissionConfigJson.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(commissionConfigJson);
            if (!node.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commission config must be a JSON object");
            }
            String type = node.path("type").asText(null);
            if (type != null && !type.isBlank()
                && !"PERCENT".equalsIgnoreCase(type)
                && !"FIXED".equalsIgnoreCase(type)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Commission type must be PERCENT or FIXED"
                );
            }
            if (node.has("rate") && !node.get("rate").isNull()) {
                BigDecimal rate = new BigDecimal(node.get("rate").asText());
                BigDecimal maxRate = merchantProperties.getCommissionMaxRate();
                if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(maxRate) > 0) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Commission rate must be between 0 and " + maxRate
                    );
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid commission config JSON");
        }
    }

    public void validateEligibleCategoriesJson(String eligibleCategoriesJson) {
        if (eligibleCategoriesJson == null || eligibleCategoriesJson.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(eligibleCategoriesJson);
            if (!node.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eligible categories must be a JSON array");
            }
            if (node.size() > 50) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many eligible categories (max 50)");
            }
            for (JsonNode item : node) {
                if (!item.isTextual() || item.asText().isBlank()) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each eligible category must be a non-empty string code"
                    );
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid eligible categories JSON");
        }
    }

    public void validateCapabilitiesJson(String capabilitiesJson) {
        if (capabilitiesJson == null || capabilitiesJson.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(capabilitiesJson);
            if (!node.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capabilities must be a JSON array");
            }
            for (JsonNode item : node) {
                if (!item.isTextual() || item.asText().isBlank()) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Each capability must be a non-empty string");
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid capabilities JSON");
        }
    }

    public void validateMerchantCampaignBudget(BigDecimal budgetTotal) {
        if (budgetTotal == null) {
            return;
        }
        if (budgetTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign budget cannot be negative");
        }
        BigDecimal max = merchantProperties.getMaxMerchantCampaignBudget();
        if (max.signum() > 0 && budgetTotal.compareTo(max) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Merchant campaign budget cannot exceed " + max
            );
        }
    }
}
