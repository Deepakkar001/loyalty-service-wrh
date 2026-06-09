package com.loyaltyos.rewards.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.voucher.entity.VoucherBatch;
import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.repository.VoucherBatchRepository;
import com.loyaltyos.voucher.repository.VoucherDenominationMappingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the effective reward catalog by merging all {@code programme_config} history rows from MySQL,
 * optional legacy {@code tenant_config.programme_config}, and voucher inventory metadata.
 */
@Service
public class RewardCatalogDbMergeService {

    private static final String DEFAULT_PROGRAMME_UID = "default";
    private static final List<RewardCatalogTypeDefinition> DEFAULT_TYPES = List.of(
        new RewardCatalogTypeDefinition("VOUCHER", "Voucher", "Discount or gift voucher"),
        new RewardCatalogTypeDefinition("DISCOUNT", "Discount", "Percentage or fixed discount"),
        new RewardCatalogTypeDefinition("PHYSICAL", "Physical gift", "Shipped or in-store item"),
        new RewardCatalogTypeDefinition("EXPERIENCE", "Experience", "Event or service reward"),
        new RewardCatalogTypeDefinition("CUSTOM", "Custom", "Tenant-defined fulfillment")
    );

    private final ProgrammeConfigRepository programmeConfigRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final ObjectProvider<VoucherBatchRepository> voucherBatchRepository;
    private final ObjectProvider<VoucherDenominationMappingRepository> denominationMappingRepository;
    private final ObjectMapper objectMapper;

    public RewardCatalogDbMergeService(
        ProgrammeConfigRepository programmeConfigRepository,
        TenantConfigRepository tenantConfigRepository,
        ObjectProvider<VoucherBatchRepository> voucherBatchRepository,
        ObjectProvider<VoucherDenominationMappingRepository> denominationMappingRepository,
        ObjectMapper objectMapper
    ) {
        this.programmeConfigRepository = Objects.requireNonNull(programmeConfigRepository);
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository);
        this.voucherBatchRepository = Objects.requireNonNull(voucherBatchRepository);
        this.denominationMappingRepository = Objects.requireNonNull(denominationMappingRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public MergedRewardCatalogResult mergeFromDatabase(String tenantId, String programmeUid) {
        String programme = normalizeProgramme(programmeUid);
        Map<String, RewardCatalogItem> itemsByUid = new LinkedHashMap<>();
        List<RewardCatalogTypeDefinition> rewardTypes = new ArrayList<>(DEFAULT_TYPES);
        int version = 1;
        Set<Integer> mergedVersions = new LinkedHashSet<>();

        List<ProgrammeConfig> history =
            programmeConfigRepository.findByTenantIdAndProgrammeUidOrderByConfigVersionAsc(tenantId, programme);
        for (ProgrammeConfig row : history) {
            mergeConfigJsonInto(row.getConfigJson(), itemsByUid, rewardTypes, mergedVersions, row.getConfigVersion());
            if (row.getConfigVersion() != null && !itemsByUid.isEmpty()) {
                version = Math.max(version, row.getConfigVersion());
            }
        }

        if (DEFAULT_PROGRAMME_UID.equals(programme)) {
            tenantConfigRepository.findByTenantId(tenantId)
                .map(TenantConfig::getProgrammeConfig)
                .ifPresent(json -> mergeConfigJsonInto(json, itemsByUid, rewardTypes, mergedVersions, null));
        }

        Set<String> synthesizedUids = new LinkedHashSet<>();
        VoucherBatchRepository batchRepo = voucherBatchRepository.getIfAvailable();
        if (batchRepo != null) {
            List<VoucherBatch> batches =
                batchRepo.findByTenantIdAndProgrammeUidOrderByUploadedAtDesc(tenantId, programme);
            for (VoucherBatch batch : batches) {
                String uid = batch.getCatalogRewardUid() == null ? "" : batch.getCatalogRewardUid().trim();
                if (uid.isEmpty() || itemsByUid.containsKey(uid)) {
                    continue;
                }
                itemsByUid.put(uid, synthesizeVoucherItem(tenantId, uid, batches));
                synthesizedUids.add(uid);
            }
        }

        List<RewardCatalogItem> items = new ArrayList<>(itemsByUid.values());
        items.sort(Comparator.comparingInt(RewardCatalogItem::displayOrder).thenComparing(RewardCatalogItem::rewardUid));

        int activeConfigVersion = programmeConfigRepository
            .findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(tenantId, programme)
            .map(ProgrammeConfig::getConfigVersion)
            .filter(v -> v != null)
            .orElse(0);

        int voucherBatchCount = 0;
        if (batchRepo != null) {
            voucherBatchCount = batchRepo.findByTenantIdAndProgrammeUidOrderByUploadedAtDesc(tenantId, programme).size();
        }

        RewardCatalogSnapshot snapshot = new RewardCatalogSnapshot(version, List.copyOf(rewardTypes), List.copyOf(items));
        return new MergedRewardCatalogResult(
            snapshot,
            activeConfigVersion,
            List.copyOf(mergedVersions),
            List.copyOf(synthesizedUids),
            voucherBatchCount,
            toRewardCatalogJsonNode(snapshot)
        );
    }

    public JsonNode toRewardCatalogJsonNode(RewardCatalogSnapshot snapshot) {
        ObjectNode catalog = objectMapper.createObjectNode();
        catalog.put("version", snapshot.version());

        ArrayNode types = objectMapper.createArrayNode();
        for (RewardCatalogTypeDefinition type : snapshot.rewardTypes()) {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("typeCode", type.typeCode());
            row.put("label", type.label());
            row.put("description", type.description());
            types.add(row);
        }
        catalog.set("rewardTypes", types);

        ArrayNode items = objectMapper.createArrayNode();
        for (RewardCatalogItem item : snapshot.items()) {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("rewardUid", item.rewardUid());
            row.put("name", item.name());
            row.put("rewardType", item.rewardType());
            row.put("status", item.status());
            row.put("pointsCost", item.pointsCost());
            row.put("displayOrder", item.displayOrder());
            row.put("description", item.description());
            if (item.metadata() != null && !item.metadata().isEmpty()) {
                row.set("metadata", objectMapper.valueToTree(item.metadata()));
            } else {
                row.set("metadata", objectMapper.createObjectNode());
            }
            items.add(row);
        }
        catalog.set("items", items);
        return catalog;
    }

    private void mergeConfigJsonInto(
        String configJson,
        Map<String, RewardCatalogItem> itemsByUid,
        List<RewardCatalogTypeDefinition> rewardTypes,
        Set<Integer> mergedVersions,
        Integer configVersion
    ) {
        if (configJson == null || configJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(configJson);
            JsonNode catalog = root.path("rewardCatalog");
            if (catalog.isMissingNode() || catalog.isNull()) {
                return;
            }

            List<RewardCatalogTypeDefinition> parsedTypes = parseTypes(catalog.path("rewardTypes"));
            if (!parsedTypes.isEmpty()) {
                rewardTypes.clear();
                rewardTypes.addAll(parsedTypes);
            }

            List<RewardCatalogItem> parsedItems = parseItemsLenient(catalog.path("items"));
            if (parsedItems.isEmpty()) {
                return;
            }
            if (configVersion != null) {
                mergedVersions.add(configVersion);
            }
            for (RewardCatalogItem item : parsedItems) {
                itemsByUid.put(item.rewardUid(), item);
            }
        } catch (JsonProcessingException ignored) {
            // Skip malformed historical row.
        }
    }

    private List<RewardCatalogTypeDefinition> parseTypes(JsonNode typesArr) {
        List<RewardCatalogTypeDefinition> out = new ArrayList<>();
        if (!typesArr.isArray()) {
            return out;
        }
        for (JsonNode t : typesArr) {
            String code = t.path("typeCode").asText("").trim();
            if (code.isEmpty()) {
                continue;
            }
            out.add(new RewardCatalogTypeDefinition(
                code,
                t.path("label").asText(code),
                t.path("description").asText("")
            ));
        }
        return out;
    }

    private List<RewardCatalogItem> parseItemsLenient(JsonNode itemsArr) {
        List<RewardCatalogItem> out = new ArrayList<>();
        if (!itemsArr.isArray()) {
            return out;
        }
        int idx = 0;
        for (JsonNode item : itemsArr) {
            String uid = item.path("rewardUid").asText("").trim();
            if (uid.isEmpty()) {
                continue;
            }
            BigDecimal pointsCost = readPoints(item.path("pointsCost"));
            if (pointsCost == null) {
                pointsCost = BigDecimal.valueOf(100);
            }
            String status = item.path("status").asText("DRAFT").trim();
            if (status.isEmpty()) {
                status = "DRAFT";
            }
            out.add(new RewardCatalogItem(
                uid,
                item.path("name").asText(uid),
                item.path("rewardType").asText("CUSTOM").trim().isEmpty()
                    ? "CUSTOM"
                    : item.path("rewardType").asText("CUSTOM").trim(),
                status,
                pointsCost,
                item.path("displayOrder").asInt(idx),
                item.path("description").asText(""),
                readMetadata(item.path("metadata"))
            ));
            idx++;
        }
        return out;
    }

    private RewardCatalogItem synthesizeVoucherItem(
        String tenantId,
        String catalogRewardUid,
        List<VoucherBatch> programmeBatches
    ) {
        BigDecimal pointsCost = BigDecimal.valueOf(100);
        VoucherDenominationMappingRepository mappingRepo = denominationMappingRepository.getIfAvailable();
        if (mappingRepo != null) {
            List<VoucherDenominationMapping> tiers =
                mappingRepo.findByTenantIdAndCatalogRewardUidAndActiveTrueOrderByPriorityAsc(tenantId, catalogRewardUid);
            Optional<BigDecimal> minPoints = tiers.stream()
                .map(VoucherDenominationMapping::getPointsRequired)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo);
            if (minPoints.isPresent()) {
                pointsCost = minPoints.get();
            }
        }

        boolean hasImported = programmeBatches.stream()
            .filter(b -> catalogRewardUid.equals(b.getCatalogRewardUid()))
            .anyMatch(b -> b.getImportedCount() > 0);

        String displayName = humanizeUid(catalogRewardUid);
        Map<String, Object> metadata = Map.of("synthesizedFromInventory", true);
        return new RewardCatalogItem(
            catalogRewardUid,
            displayName,
            "VOUCHER",
            hasImported ? "ACTIVE" : "DRAFT",
            pointsCost,
            programmeBatches.size(),
            "Recovered from voucher batch history in MySQL",
            metadata
        );
    }

    private static String humanizeUid(String uid) {
        String trimmed = uid == null ? "" : uid.trim();
        if (trimmed.isEmpty()) {
            return "Voucher reward";
        }
        return trimmed.replace('_', ' ');
    }

    private static BigDecimal readPoints(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        if (n.isTextual()) {
            try {
                return new BigDecimal(n.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Map<String, Object> readMetadata(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull() || !n.isObject()) {
            return Map.of();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        n.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            if (v.isTextual()) {
                m.put(e.getKey(), v.asText());
            } else if (v.isNumber()) {
                m.put(e.getKey(), v.numberValue());
            } else if (v.isBoolean()) {
                m.put(e.getKey(), v.asBoolean());
            } else if (!v.isNull()) {
                m.put(e.getKey(), v.toString());
            }
        });
        return Map.copyOf(m);
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? DEFAULT_PROGRAMME_UID : programmeUid.trim();
    }

    public record MergedRewardCatalogResult(
        RewardCatalogSnapshot catalog,
        int activeConfigVersion,
        List<Integer> mergedConfigVersions,
        List<String> synthesizedRewardUids,
        int voucherBatchCount,
        JsonNode rewardCatalogJson
    ) {}
}
