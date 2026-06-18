package com.loyaltyos.merchants.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.merchants.dto.CreateSettlementDisputeRequest;
import com.loyaltyos.merchants.dto.MerchantSettlementCycleResponse;
import com.loyaltyos.merchants.dto.SettlementLineItemResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantSettlementCycle;
import com.loyaltyos.merchants.entity.SettlementDispute;
import com.loyaltyos.merchants.entity.SettlementLineItem;
import com.loyaltyos.merchants.enums.MerchantSettlementCycleStatus;
import com.loyaltyos.merchants.enums.SettlementDisputeStatus;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.repository.MerchantSettlementCycleRepository;
import com.loyaltyos.merchants.repository.SettlementDisputeRepository;
import com.loyaltyos.merchants.repository.SettlementLineItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantSettlementService {

    private final MerchantRepository merchantRepository;
    private final MerchantSettlementCycleRepository cycleRepository;
    private final SettlementLineItemRepository lineItemRepository;
    private final SettlementDisputeRepository disputeRepository;
    private final CampaignRepository campaignRepository;

    public MerchantSettlementService(
        MerchantRepository merchantRepository,
        MerchantSettlementCycleRepository cycleRepository,
        SettlementLineItemRepository lineItemRepository,
        SettlementDisputeRepository disputeRepository,
        CampaignRepository campaignRepository
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.cycleRepository = Objects.requireNonNull(cycleRepository, "cycleRepository");
        this.lineItemRepository = Objects.requireNonNull(lineItemRepository, "lineItemRepository");
        this.disputeRepository = Objects.requireNonNull(disputeRepository, "disputeRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
    }

    @Transactional(readOnly = true)
    public List<MerchantSettlementCycleResponse> listCycles(String tenantId, String merchantUid) {
        getMerchantOrThrow(tenantId, merchantUid);
        return cycleRepository.findByTenantIdAndMerchantUidOrderByPeriodEndDesc(tenantId, merchantUid)
            .stream()
            .map(this::toCycleResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public MerchantSettlementCycleResponse getCycle(String tenantId, String merchantUid, String cycleUid) {
        MerchantSettlementCycle cycle = loadCycle(tenantId, merchantUid, cycleUid);
        return toCycleResponse(cycle);
    }

    @Transactional(readOnly = true)
    public List<SettlementLineItemResponse> listLineItems(
        String tenantId,
        String merchantUid,
        String cycleUid
    ) {
        MerchantSettlementCycle cycle = loadCycle(tenantId, merchantUid, cycleUid);
        return lineItemRepository.findByCycleIdOrderByCreatedAtAsc(cycle.getId())
            .stream()
            .map(this::toLineItemResponse)
            .toList();
    }

    @Transactional
    public MerchantSettlementCycleResponse generateMonthlyCycle(
        String tenantId,
        String merchantUid,
        YearMonth period
    ) {
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        if (cycleRepository.existsByTenantIdAndMerchantUidAndPeriodStartAndPeriodEnd(
            tenantId, merchantUid, periodStart, periodEnd)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Settlement statement already exists for period " + period
            );
        }

        MerchantSettlementCycle cycle = new MerchantSettlementCycle();
        cycle.setCycleUid("MSC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        cycle.setTenantId(tenantId);
        cycle.setMerchantUid(merchantUid);
        cycle.setPeriodStart(periodStart);
        cycle.setPeriodEnd(periodEnd);
        cycle.setStatus(MerchantSettlementCycleStatus.PENDING_FINANCE);
        cycleRepository.save(cycle);

        List<Campaign> campaigns = campaignRepository
            .findByTenantIdAndMerchantIdOrderByCreatedAtDesc(tenantId, merchantUid);
        long totalPoints = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Campaign campaign : campaigns) {
            BigDecimal consumed = campaign.getBudgetConsumed();
            if (consumed == null || consumed.signum() <= 0) {
                continue;
            }
            SettlementLineItem line = new SettlementLineItem();
            line.setLineItemUid("SLI_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            line.setCycle(cycle);
            line.setTxnReference("CAMP:" + campaign.getCampaignUid());
            line.setMonetaryValue(consumed);
            long points = consumed.longValue();
            line.setPointsAmount(points);
            lineItemRepository.save(line);
            totalPoints += points;
            totalValue = totalValue.add(consumed);
        }

        cycle.setTotalPoints(totalPoints);
        cycle.setTotalMonetaryValue(totalValue);
        cycleRepository.save(cycle);

        if (merchant.isSettlementHold()) {
            cycle.setStatus(MerchantSettlementCycleStatus.DRAFT);
            cycleRepository.save(cycle);
        }

        return toCycleResponse(cycle);
    }

    @Transactional
    public MerchantSettlementCycleResponse finalizeCycle(
        String tenantId,
        String merchantUid,
        String cycleUid,
        String actorEmail
    ) {
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        if (merchant.isSettlementHold()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Settlement is on hold while merchant is suspended"
            );
        }
        MerchantSettlementCycle cycle = loadCycle(tenantId, merchantUid, cycleUid);
        if (cycle.getStatus() == MerchantSettlementCycleStatus.FINAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement cycle is already final");
        }
        long openDisputes = disputeRepository.findByStatusOrderByCreatedAtDesc(SettlementDisputeStatus.OPEN)
            .stream()
            .filter(d -> d.getLineItem().getCycle().getId().equals(cycle.getId()))
            .count();
        if (openDisputes > 0) {
            cycle.setStatus(MerchantSettlementCycleStatus.DISPUTED);
        } else {
            cycle.setStatus(MerchantSettlementCycleStatus.FINAL);
            cycle.setFinalizedAt(Instant.now());
        }
        cycleRepository.save(cycle);
        return toCycleResponse(cycle);
    }

    @Transactional
    public SettlementDispute createDispute(
        String tenantId,
        String merchantUid,
        String cycleUid,
        CreateSettlementDisputeRequest request
    ) {
        MerchantSettlementCycle cycle = loadCycle(tenantId, merchantUid, cycleUid);
        if (cycle.getStatus() == MerchantSettlementCycleStatus.FINAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot dispute a finalized statement");
        }
        SettlementLineItem lineItem = lineItemRepository.findByLineItemUid(request.getLineItemUid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Line item not found"));
        if (!lineItem.getCycle().getId().equals(cycle.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line item does not belong to this cycle");
        }
        if (lineItem.isDisputed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Line item already disputed");
        }
        lineItem.setDisputed(true);
        lineItemRepository.save(lineItem);

        SettlementDispute dispute = new SettlementDispute();
        dispute.setDisputeUid("SDP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        dispute.setLineItem(lineItem);
        dispute.setReason(request.getReason().trim());
        dispute.setHoldAmount(lineItem.getMonetaryValue());
        dispute.setStatus(SettlementDisputeStatus.OPEN);
        disputeRepository.save(dispute);

        cycle.setStatus(MerchantSettlementCycleStatus.DISPUTED);
        cycleRepository.save(cycle);
        return dispute;
    }

    private Merchant getMerchantOrThrow(String tenantId, String merchantUid) {
        return merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
    }

    private MerchantSettlementCycle loadCycle(String tenantId, String merchantUid, String cycleUid) {
        MerchantSettlementCycle cycle = cycleRepository.findByTenantIdAndCycleUid(tenantId, cycleUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement cycle not found"));
        if (!merchantUid.equals(cycle.getMerchantUid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Settlement cycle does not belong to merchant");
        }
        return cycle;
    }

    private MerchantSettlementCycleResponse toCycleResponse(MerchantSettlementCycle cycle) {
        MerchantSettlementCycleResponse response = new MerchantSettlementCycleResponse();
        response.setCycleUid(cycle.getCycleUid());
        response.setMerchantUid(cycle.getMerchantUid());
        response.setPeriodStart(cycle.getPeriodStart());
        response.setPeriodEnd(cycle.getPeriodEnd());
        response.setStatus(cycle.getStatus().name());
        response.setTotalPoints(cycle.getTotalPoints());
        response.setTotalMonetaryValue(cycle.getTotalMonetaryValue());
        response.setCreatedAt(cycle.getCreatedAt());
        response.setFinalizedAt(cycle.getFinalizedAt());
        List<SettlementLineItem> lines = lineItemRepository.findByCycleIdOrderByCreatedAtAsc(cycle.getId());
        response.setLineItemCount(lines.size());
        long openDisputes = lines.stream()
            .filter(SettlementLineItem::isDisputed)
            .count();
        response.setOpenDisputeCount((int) openDisputes);
        return response;
    }

    private SettlementLineItemResponse toLineItemResponse(SettlementLineItem line) {
        SettlementLineItemResponse response = new SettlementLineItemResponse();
        response.setLineItemUid(line.getLineItemUid());
        response.setTxnReference(line.getTxnReference());
        response.setPointsAmount(line.getPointsAmount());
        response.setMonetaryValue(line.getMonetaryValue());
        response.setDisputed(line.isDisputed());
        response.setCreatedAt(line.getCreatedAt());
        return response;
    }
}
