package com.loyaltyos.campaigns.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignProgrammeValidatorTest {

    @Mock
    private ProgrammeRepository programmeRepository;
    @Mock
    private ProgrammeService programmeService;
    @Mock
    private TierDefinitionRepository tierDefinitionRepository;

    private CampaignProgrammeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CampaignProgrammeValidator(
            programmeRepository,
            programmeService,
            tierDefinitionRepository,
            new ObjectMapper()
        );
    }

    @Test
    void resolveTierDefinitions_fallsBackToLegacyNullProgrammeUidForDefault() {
        TierDefinition legacy = new TierDefinition();
        legacy.setTenantId("t1");
        legacy.setTierUid("tier-gold");
        legacy.setName("Gold");

        when(tierDefinitionRepository.findByTenantIdAndProgrammeUidOrderByRankOrderAsc("t1", "default"))
            .thenReturn(List.of());
        when(tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc("t1"))
            .thenReturn(List.of(legacy));

        List<TierDefinition> resolved = validator.resolveTierDefinitions("t1", "default");

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().getTierUid()).isEqualTo("tier-gold");
    }

    @Test
    void validateTierUids_acceptsLegacyTierForDefaultProgramme() {
        TierDefinition legacy = new TierDefinition();
        legacy.setTenantId("t1");
        legacy.setTierUid("tier-gold");

        when(tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc("t1"))
            .thenReturn(List.of(legacy));

        validator.validateTierUids("t1", "default", new CampaignTargetSegment(List.of("tier-gold"), null, null, null));
    }

    @Test
    void validateTierUids_acceptsTierFromTenantDefinitions() {
        TierDefinition row = new TierDefinition();
        row.setTenantId("t1");
        row.setTierUid("gold-tier");
        row.setName("Gold");

        when(tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc("t1"))
            .thenReturn(List.of(row));

        validator.validateTierUids(
            "t1",
            "default",
            new CampaignTargetSegment(List.of("gold-tier"), null, null, null)
        );
    }

    @Test
    void extractConfiguredEventTypes_readsEventDefinitionsFromProgrammeConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(
            "{\"eventSchema\":{\"eventDefinitions\":[{\"eventType\":\"ORDER_PLACED\"},{\"eventType\":\"REFUND\"}]}}"
        );
        Set<String> types = CampaignProgrammeValidator.extractConfiguredEventTypes(root);
        assertThat(types).containsExactlyInAnyOrder("ORDER_PLACED", "REFUND");
    }

    @Test
    void extractConfiguredEventTypes_legacyStandardFieldsOnly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(
            "{\"eventSchema\":{\"standardFields\":[{\"name\":\"customerId\",\"type\":\"string\"}]}}"
        );
        Set<String> types = CampaignProgrammeValidator.extractConfiguredEventTypes(root);
        assertThat(types).containsExactly("PURCHASE");
    }
}
