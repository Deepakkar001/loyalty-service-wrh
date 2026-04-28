package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.response.OnboardingMetadataResponse;
import com.loyaltyos.onboarding.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/onboarding")
@Tag(name = "Tenant Onboarding", description = "Tenant registration and onboarding workflow")
@RequiredArgsConstructor
public class MetadataController {

    private final RefBusinessCategoryRepository businessCategoryRepository;
    private final RefCountryRepository countryRepository;
    private final RefTimezoneRepository timezoneRepository;
    private final RefBusinessModelRepository businessModelRepository;
    private final RefAnnualRevenueRangeRepository annualRevenueRangeRepository;
    private final RefPaymentMethodAcceptedRepository paymentMethodAcceptedRepository;
    private final RefSettlementFrequencyRepository settlementFrequencyRepository;
    private final RefCurrencyRepository currencyRepository;
    private final RefBillingPaymentMethodRepository billingPaymentMethodRepository;
    private final RefContractDurationRepository contractDurationRepository;

    @GetMapping("/metadata")
    @Operation(summary = "Get onboarding dropdown metadata",
        description = "Returns allowed values for all dropdowns used in onboarding UI")
    public ResponseEntity<OnboardingMetadataResponse> getMetadata() {
        var businessCategories = businessCategoryRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder()
                .value(v.getCode())
                .label(v.getLabel())
                .build())
            .toList();

        var countries = countryRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder()
                .value(normalizeCountryCode(v.getCode()))
                .label(v.getLabel())
                .build())
            .toList();

        var timezones = timezoneRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var businessModels = businessModelRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var annualRevenueRanges = annualRevenueRangeRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var paymentMethodsAccepted = paymentMethodAcceptedRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var settlementFrequencies = settlementFrequencyRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var currencies = currencyRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var billingPaymentMethods = billingPaymentMethodRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        var contractDurations = contractDurationRepository.findByActiveTrueOrderBySortOrderAscLabelAsc()
            .stream()
            .map(v -> OnboardingMetadataResponse.Option.builder().value(v.getCode()).label(v.getLabel()).build())
            .toList();

        return ResponseEntity.ok(OnboardingMetadataResponse.builder()
            .businessCategories(businessCategories)
            .countries(countries)
            .timezones(timezones)
            .businessModels(businessModels)
            .annualRevenueRanges(annualRevenueRanges)
            .paymentMethodsAccepted(paymentMethodsAccepted)
            .settlementFrequencies(settlementFrequencies)
            .currencies(currencies)
            .billingPaymentMethods(billingPaymentMethods)
            .contractDurations(contractDurations)
            .build());
    }

    private static String normalizeCountryCode(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
