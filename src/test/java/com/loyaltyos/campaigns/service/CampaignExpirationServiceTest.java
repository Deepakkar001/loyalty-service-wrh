package com.loyaltyos.campaigns.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CampaignExpirationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CampaignExpirationService expirationService;

    @BeforeEach
    void setUp() {
        expirationService = new CampaignExpirationService(jdbcTemplate);
    }

    @Test
    void sweepCampaignsPastValidUntil_updatesRows() {
        when(jdbcTemplate.update(anyString())).thenReturn(2);

        int updated = expirationService.sweepCampaignsPastValidUntil();

        assertThat(updated).isEqualTo(2);
        verify(jdbcTemplate).update(anyString());
    }
}
