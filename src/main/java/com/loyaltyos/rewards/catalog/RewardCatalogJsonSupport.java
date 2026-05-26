package com.loyaltyos.rewards.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RewardCatalogJsonSupport {

    private RewardCatalogJsonSupport() {}

    public static RewardCatalogSnapshot parseFromProgrammeRoot(JsonNode programmeRoot) {
        if (programmeRoot == null || programmeRoot.isMissingNode()) {
            return RewardCatalogSnapshot.empty();
        }
        JsonNode catalog = programmeRoot.path("rewardCatalog");
        if (catalog.isMissingNode() || catalog.isNull()) {
            return RewardCatalogSnapshot.empty();
        }
        int version = catalog.path("version").asInt(1);
        List<RewardCatalogTypeDefinition> types = parseTypes(catalog.path("rewardTypes"));
        List<RewardCatalogItem> items = parseItems(catalog.path("items"), types);
        return new RewardCatalogSnapshot(version, types, items);
    }

    private static List<RewardCatalogTypeDefinition> parseTypes(JsonNode typesArr) {
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

    private static List<RewardCatalogItem> parseItems(JsonNode itemsArr, List<RewardCatalogTypeDefinition> types) {
        List<RewardCatalogItem> out = new ArrayList<>();
        if (!itemsArr.isArray()) {
            return out;
        }
        for (JsonNode item : itemsArr) {
            String uid = item.path("rewardUid").asText("").trim();
            if (uid.isEmpty()) {
                continue;
            }
            String rewardType = item.path("rewardType").asText("CUSTOM").trim();
            if (rewardType.isEmpty()) {
                rewardType = "CUSTOM";
            }
            BigDecimal pointsCost = readPoints(item.path("pointsCost"));
            if (pointsCost == null) {
                continue;
            }
            Map<String, Object> metadata = readMetadata(item.path("metadata"));
            out.add(new RewardCatalogItem(
                uid,
                item.path("name").asText(uid),
                rewardType,
                item.path("status").asText("DRAFT"),
                pointsCost,
                item.path("displayOrder").asInt(0),
                item.path("description").asText(""),
                metadata
            ));
        }
        return out;
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
}
