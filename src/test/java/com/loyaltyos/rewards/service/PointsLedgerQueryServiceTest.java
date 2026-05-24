package com.loyaltyos.rewards.service;

import com.loyaltyos.rewards.dto.LedgerTransactionDto;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.rules.enums.LedgerEntryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsLedgerQueryServiceTest {

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @InjectMocks
    private PointsLedgerQueryService service;

    @Test
    void listCustomerTransactions_usesSimpleQueryWhenNoFilters() {
        PointsLedger row = PointsLedger.builder()
            .id(1L)
            .tenantId("t1")
            .programmeUid("default")
            .customerId("c1")
            .entryType(LedgerEntryType.CREDIT)
            .points(BigDecimal.TEN)
            .sourceEventId("evt_1")
            .description("earn")
            .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
            .build();
        when(pointsLedgerRepository.findByTenantIdAndProgrammeUidAndCustomerIdOrderByCreatedAtDesc(
            eq("t1"), eq("default"), eq("c1"), eq(PageRequest.of(0, 10))
        )).thenReturn(new PageImpl<>(List.of(row)));

        var page = service.listCustomerTransactions(
            "t1", "default", "c1", null, null, null, PageRequest.of(0, 10)
        );
        assertThat(page.getTotalElements()).isOne();
        LedgerTransactionDto dto = page.getContent().get(0);
        assertThat(dto.getLedgerId()).isEqualTo(1L);
        assertThat(dto.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(dto.getPoints()).isEqualByComparingTo("10");
    }

    @Test
    void listCustomerTransactions_usesFilteredQueryWhenFiltersPresent() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        when(pointsLedgerRepository.findCustomerLedger(
            eq("t1"), eq("default"), eq("c1"), eq(LedgerEntryType.DEBIT), eq(from), isNull(),
            eq(PageRequest.of(0, 5))
        )).thenReturn(new PageImpl<>(List.of()));

        var page = service.listCustomerTransactions(
            "t1", null, "c1", LedgerEntryType.DEBIT, from, null, PageRequest.of(0, 5)
        );
        assertThat(page.isEmpty()).isTrue();
        verify(pointsLedgerRepository).findCustomerLedger(
            eq("t1"), eq("default"), eq("c1"), eq(LedgerEntryType.DEBIT), eq(from), isNull(),
            eq(PageRequest.of(0, 5))
        );
    }
}
