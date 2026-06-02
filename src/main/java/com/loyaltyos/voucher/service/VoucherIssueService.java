package com.loyaltyos.voucher.service;

import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.rewards.dto.RedemptionRequest;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rewards.service.RewardRedemptionService;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.entity.VoucherInventory;
import com.loyaltyos.voucher.enums.VoucherAuditEventType;
import com.loyaltyos.voucher.enums.VoucherStatus;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.exception.VoucherOutOfStockException;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherIssueService {

    private static final Logger log = LoggerFactory.getLogger(VoucherIssueService.class);
    private static final String VOUCHER_TYPE = "VOUCHER";

    private final VoucherInventoryRepository inventoryRepository;
    private final VoucherCodeCryptoService cryptoService;
    private final RewardRedemptionService redemptionService;
    private final RewardCatalogService rewardCatalogService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final VoucherAuditService auditService;
    private final VoucherDenominationMappingService denominationMappingService;

    public VoucherIssueService(
        VoucherInventoryRepository inventoryRepository,
        VoucherCodeCryptoService cryptoService,
        RewardRedemptionService redemptionService,
        RewardCatalogService rewardCatalogService,
        PointsLedgerRepository pointsLedgerRepository,
        VoucherAuditService auditService,
        VoucherDenominationMappingService denominationMappingService
    ) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.cryptoService = Objects.requireNonNull(cryptoService);
        this.redemptionService = Objects.requireNonNull(redemptionService);
        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService);
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository);
        this.auditService = Objects.requireNonNull(auditService);
        this.denominationMappingService = Objects.requireNonNull(denominationMappingService);
    }

    @Transactional
    public VoucherIssueResponse issueVoucher(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String customerId,
        String redemptionId
    ) {
        return issueVoucher(tenantId, programmeUid, catalogRewardUid, customerId, redemptionId, null, null);
    }

    @Transactional
    public VoucherIssueResponse issueVoucher(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String customerId,
        String redemptionId,
        BigDecimal pointsToRedeem,
        BigDecimal faceValue
    ) {
        String programme = normalizeProgramme(programmeUid);
        String catalogUid = catalogRewardUid.trim();
        String customer = customerId.trim();
        String redemption = redemptionId.trim();

        if (denominationMappingService.hasActiveMappings(tenantId, catalogUid)) {
            return issueWithDenomination(
                tenantId, programme, catalogUid, customer, redemption, pointsToRedeem, faceValue
            );
        }
        if (pointsToRedeem != null || faceValue != null) {
            throw new VoucherCatalogException(
                "pointsToRedeem/faceValue require denomination mappings on catalog " + catalogUid
            );
        }
        return issueSinglePool(tenantId, programme, catalogUid, customer, redemption);
    }

    private VoucherIssueResponse issueWithDenomination(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String customerId,
        String redemptionId,
        BigDecimal pointsToRedeem,
        BigDecimal faceValue
    ) {
        Optional<VoucherInventory> existingInventory = inventoryRepository.findByRedemptionId(redemptionId);
        if (existingInventory.isPresent()) {
            return buildSuccessResponse(
                existingInventory.get(),
                tenantId,
                programmeUid,
                catalogRewardUid,
                true,
                null,
                null
            );
        }

        String idempotencyKey = "redeem:" + redemptionId;
        Optional<PointsLedger> existingLedger = pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            tenantId, customerId, idempotencyKey
        );
        if (existingLedger.isPresent()) {
            return inventoryRepository.findByRedemptionId(redemptionId)
                .map(inv -> buildSuccessResponse(inv, tenantId, programmeUid, catalogRewardUid, true, null, null))
                .orElseGet(() -> ledgerOnlyReplayResponse(
                    existingLedger.get(), tenantId, programmeUid, catalogRewardUid, redemptionId
                ));
        }

        validateCatalogVoucher(tenantId, programmeUid, catalogRewardUid);
        VoucherDenominationMapping mapping = denominationMappingService.resolveMapping(
            tenantId, catalogRewardUid, pointsToRedeem, faceValue
        );
        denominationMappingService.assertStockForMapping(tenantId, programmeUid, mapping);

        Long lockedId = inventoryRepository.lockNextAvailableIdByFaceValue(
            tenantId, programmeUid, catalogRewardUid, mapping.getFaceValue()
        ).orElseThrow(() -> new VoucherOutOfStockException(
            "No available vouchers for face value " + mapping.getFaceValue().toPlainString()
        ));

        VoucherInventory inventory = inventoryRepository.findById(lockedId)
            .orElseThrow(() -> new VoucherOutOfStockException("Locked voucher row not found"));

        inventory.setStatus(VoucherStatus.ISSUED);
        inventory.setCustomerId(customerId);
        inventory.setRedemptionId(redemptionId);
        inventory.setDenominationMappingId(mapping.getId());
        inventory.setIssuedAt(Instant.now());
        inventoryRepository.saveAndFlush(inventory);

        try {
            RedemptionRequest request = new RedemptionRequest();
            request.setRedemptionId(redemptionId);
            request.setCustomerId(customerId);
            request.setProgrammeUid(programmeUid);
            request.setCatalogRewardUid(catalogRewardUid);
            request.setPointsToRedeem(mapping.getPointsRequired());
            request.setChannel("VOUCHER_ISSUANCE_MIXED");

            RedemptionResult redemptionResult = redemptionService.redeem(tenantId, request);
            inventory.setLedgerId(redemptionResult.getLedgerId());
            inventoryRepository.save(inventory);

            auditService.logEvent(
                tenantId,
                VoucherAuditEventType.ISSUED,
                inventory.getInventoryUid(),
                customerId,
                redemptionId,
                redemptionResult.getLedgerId(),
                "Mixed-denomination voucher issued",
                Map.of(
                    "points_redeemed", mapping.getPointsRequired(),
                    "face_value", mapping.getFaceValue(),
                    "mapping_uid", mapping.getMappingUid()
                )
            );

            return buildSuccessResponse(
                inventory,
                tenantId,
                programmeUid,
                catalogRewardUid,
                redemptionResult.isIdempotentReplay(),
                redemptionResult.getNewBalance(),
                mapping
            );
        } catch (RewardInsufficientBalanceException e) {
            releaseInventory(inventory);
            VoucherIssueResponse response = new VoucherIssueResponse();
            response.setStatus("INSUFFICIENT_BALANCE");
            response.setErrorMessage("Customer has insufficient points to redeem voucher");
            response.setRetryable(false);
            response.setTimestamp(Instant.now());
            return response;
        } catch (RuntimeException e) {
            releaseInventory(inventory);
            throw e;
        }
    }

    private VoucherIssueResponse issueSinglePool(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String customerId,
        String redemptionId
    ) {
        Optional<VoucherInventory> existingInventory = inventoryRepository.findByRedemptionId(redemptionId);
        if (existingInventory.isPresent()) {
            return buildSuccessResponse(
                existingInventory.get(),
                tenantId,
                programmeUid,
                catalogRewardUid,
                true,
                null,
                null
            );
        }

        String idempotencyKey = "redeem:" + redemptionId;
        Optional<PointsLedger> existingLedger = pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            tenantId, customerId, idempotencyKey
        );
        if (existingLedger.isPresent()) {
            return inventoryRepository.findByRedemptionId(redemptionId)
                .map(inv -> buildSuccessResponse(inv, tenantId, programmeUid, catalogRewardUid, true, null, null))
                .orElseGet(() -> ledgerOnlyReplayResponse(
                    existingLedger.get(), tenantId, programmeUid, catalogRewardUid, redemptionId
                ));
        }

        validateCatalogVoucher(tenantId, programmeUid, catalogRewardUid);

        Long lockedId = inventoryRepository.lockNextAvailableId(tenantId, programmeUid, catalogRewardUid)
            .orElseThrow(() -> new VoucherOutOfStockException(
                "No available vouchers for catalogue item " + catalogRewardUid
            ));

        VoucherInventory inventory = inventoryRepository.findById(lockedId)
            .orElseThrow(() -> new VoucherOutOfStockException("Locked voucher row not found"));

        inventory.setStatus(VoucherStatus.ISSUED);
        inventory.setCustomerId(customerId);
        inventory.setRedemptionId(redemptionId);
        inventory.setIssuedAt(Instant.now());
        inventoryRepository.saveAndFlush(inventory);

        try {
            RedemptionRequest request = new RedemptionRequest();
            request.setRedemptionId(redemptionId);
            request.setCustomerId(customerId);
            request.setProgrammeUid(programmeUid);
            request.setCatalogRewardUid(catalogRewardUid);
            request.setChannel("VOUCHER_ISSUANCE");

            RedemptionResult redemptionResult = redemptionService.redeem(tenantId, request);
            inventory.setLedgerId(redemptionResult.getLedgerId());
            inventoryRepository.save(inventory);

            RewardCatalogItem catalogItem = rewardCatalogService
                .findActiveItem(tenantId, programmeUid, catalogRewardUid)
                .orElseThrow(() -> new VoucherCatalogException("Catalog item not found: " + catalogRewardUid));

            auditService.logEvent(
                tenantId,
                VoucherAuditEventType.ISSUED,
                inventory.getInventoryUid(),
                customerId,
                redemptionId,
                redemptionResult.getLedgerId(),
                "Voucher issued to customer",
                Map.of(
                    "points_cost", catalogItem.pointsCost(),
                    "batch_uid", inventory.getBatchUid()
                )
            );

            return buildSuccessResponse(
                inventory,
                tenantId,
                programmeUid,
                catalogRewardUid,
                redemptionResult.isIdempotentReplay(),
                redemptionResult.getNewBalance(),
                null
            );
        } catch (RewardInsufficientBalanceException e) {
            releaseInventory(inventory);
            VoucherIssueResponse response = new VoucherIssueResponse();
            response.setStatus("INSUFFICIENT_BALANCE");
            response.setErrorMessage("Customer has insufficient points to redeem voucher");
            response.setRetryable(false);
            response.setTimestamp(Instant.now());
            return response;
        } catch (RuntimeException e) {
            releaseInventory(inventory);
            throw e;
        }
    }

    private void validateCatalogVoucher(String tenantId, String programmeUid, String catalogRewardUid) {
        RewardCatalogItem catalogItem = rewardCatalogService.findActiveItem(tenantId, programmeUid, catalogRewardUid)
            .orElseThrow(() -> new VoucherCatalogException("Catalog item not found or inactive: " + catalogRewardUid));
        if (!VOUCHER_TYPE.equalsIgnoreCase(catalogItem.rewardType())) {
            throw new VoucherCatalogException("Catalog item is not type VOUCHER: " + catalogRewardUid);
        }
    }

    private void releaseInventory(VoucherInventory inventory) {
        inventory.setStatus(VoucherStatus.AVAILABLE);
        inventory.setCustomerId(null);
        inventory.setRedemptionId(null);
        inventory.setIssuedAt(null);
        inventory.setLedgerId(null);
        inventoryRepository.save(inventory);
    }

    private VoucherIssueResponse buildSuccessResponse(
        VoucherInventory inventory,
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        boolean idempotentReplay,
        BigDecimal newBalance,
        VoucherDenominationMapping mapping
    ) {
        String code = cryptoService.decryptCode(inventory.getCodeCiphertext());
        String pin = inventory.getPinCiphertext() != null
            ? cryptoService.decryptCode(inventory.getPinCiphertext())
            : null;

        BigDecimal pointsRedeemed;
        if (mapping != null) {
            pointsRedeemed = mapping.getPointsRequired();
        } else {
            RewardCatalogItem catalogItem = rewardCatalogService.findActiveItem(tenantId, programmeUid, catalogRewardUid)
                .orElseThrow(() -> new VoucherCatalogException("Catalog item not found: " + catalogRewardUid));
            pointsRedeemed = catalogItem.pointsCost();
        }

        VoucherIssueResponse.VoucherDetails details = new VoucherIssueResponse.VoucherDetails();
        details.setCode(code);
        details.setPin(pin);
        details.setFaceValue(inventory.getFaceValue());
        details.setCurrency(inventory.getCurrency());
        details.setExpiresAt(inventory.getExpiresAt());

        VoucherIssueResponse response = new VoucherIssueResponse();
        response.setStatus("SUCCESS");
        response.setRedemptionId(inventory.getRedemptionId());
        response.setPointsRedeemed(pointsRedeemed);
        response.setNewBalance(newBalance);
        response.setLedgerId(inventory.getLedgerId());
        response.setCatalogRewardUid(catalogRewardUid);
        response.setIdempotentReplay(idempotentReplay);
        response.setVoucher(details);
        response.setTimestamp(inventory.getIssuedAt() != null ? inventory.getIssuedAt() : Instant.now());

        if (mapping != null) {
            VoucherIssueResponse.SelectedDenomination selected = new VoucherIssueResponse.SelectedDenomination();
            selected.setFaceValue(mapping.getFaceValue());
            selected.setCurrency(mapping.getCurrency());
            selected.setPointsRequired(mapping.getPointsRequired());
            response.setSelectedDenomination(selected);
        }

        return response;
    }

    private VoucherIssueResponse ledgerOnlyReplayResponse(
        PointsLedger ledger,
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String redemptionId
    ) {
        log.warn(
            "Ledger idempotency hit without voucher inventory for redemption_id={} tenant={}",
            redemptionId, tenantId
        );
        VoucherIssueResponse response = new VoucherIssueResponse();
        response.setStatus("ERROR");
        response.setRedemptionId(redemptionId);
        response.setLedgerId(ledger.getId());
        response.setPointsRedeemed(ledger.getPoints());
        response.setCatalogRewardUid(catalogRewardUid);
        response.setIdempotentReplay(true);
        response.setErrorMessage(
            "Points were debited but voucher code is not linked. Contact support with redemption id."
        );
        response.setRetryable(false);
        response.setTimestamp(ledger.getCreatedAt());
        return response;
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }
}
