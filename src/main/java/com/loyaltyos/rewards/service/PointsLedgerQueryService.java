package com.loyaltyos.rewards.service;

import com.loyaltyos.rewards.dto.LedgerTransactionDto;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.rules.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class PointsLedgerQueryService {

    private final PointsLedgerRepository pointsLedgerRepository;

    public PointsLedgerQueryService(PointsLedgerRepository pointsLedgerRepository) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
    }

    @Transactional(readOnly = true)
    public Page<LedgerTransactionDto> listCustomerTransactions(
        String tenantId,
        String programmeUid,
        String customerId,
        LedgerEntryType entryType,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        String p = normalizeProgramme(programmeUid);
        Page<PointsLedger> page;
        if (entryType != null || from != null || to != null) {
            page = pointsLedgerRepository.findCustomerLedger(
                tenantId, p, customerId, entryType, from, to, pageable
            );
        } else {
            page = pointsLedgerRepository.findByTenantIdAndProgrammeUidAndCustomerIdOrderByCreatedAtDesc(
                tenantId, p, customerId, pageable
            );
        }
        return page.map(PointsLedgerQueryService::toDto);
    }

    private static LedgerTransactionDto toDto(PointsLedger row) {
        LedgerTransactionDto dto = new LedgerTransactionDto();
        dto.setLedgerId(row.getId());
        dto.setIdempotencyKey(row.getIdempotencyKey());
        dto.setEntryType(row.getEntryType());
        dto.setPoints(row.getPoints());
        dto.setSourceEventId(row.getSourceEventId());
        dto.setDescription(row.getDescription());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid;
    }
}
