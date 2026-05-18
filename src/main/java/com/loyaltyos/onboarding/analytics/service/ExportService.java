package com.loyaltyos.onboarding.analytics.service;

import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.repository.EarnRuleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final JdbcTemplate jdbcTemplate;
    private final EarnRuleRepository earnRuleRepository;

    public ExportService(JdbcTemplate jdbcTemplate, EarnRuleRepository earnRuleRepository) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
    }

    public void streamPointsLedger(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT id, customer_id, entry_type, points, source_rule_id, created_at
            FROM points_ledger
            WHERE tenant_id = ? AND programme_uid = ? AND created_at >= ? AND created_at < ?
            ORDER BY created_at ASC
            """;
        jdbcTemplate.query(
            sql,
            rs -> {
                String line = String.join(
                    ",",
                    csvCell(rs.getString("id")),
                    csvCell(rs.getString("customer_id")),
                    csvCell(rs.getString("entry_type")),
                    csvCell(rs.getString("points")),
                    csvCell(rs.getString("source_rule_id")),
                    csvCell(rs.getString("created_at"))
                );
                lineConsumer.accept(line);
            },
            tenantId,
            programmeUid,
            fromDt,
            toDt
        );
    }

    public void streamWebhookLog(
        String tenantId,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT wdl.id, wdl.event_type, wdl.status, wdl.attempt_count, wdl.last_error, wdl.created_at
            FROM webhook_delivery_log wdl
            JOIN webhook_subscriptions ws ON ws.id = wdl.subscription_id
            WHERE wdl.tenant_id = ? AND wdl.created_at >= ? AND wdl.created_at < ?
            ORDER BY wdl.created_at ASC
            """;
        jdbcTemplate.query(
            sql,
            rs -> {
                String line = String.join(
                    ",",
                    csvCell(rs.getString("id")),
                    csvCell(rs.getString("event_type")),
                    csvCell(rs.getString("status")),
                    csvCell(rs.getString("attempt_count")),
                    csvCell(rs.getString("last_error")),
                    csvCell(rs.getString("created_at"))
                );
                lineConsumer.accept(line);
            },
            tenantId,
            fromDt,
            toDt
        );
    }

    public Map<String, Object> exportRuleConfigBundle(String tenantId, String programmeUid) {
        List<EarnRule> rules = earnRuleRepository.findByTenantIdAndProgrammeUidOrderByPriorityDesc(tenantId, programmeUid);
        List<Map<String, Object>> rows = rules.stream().map(rule -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ruleUid", rule.getRuleUid());
            m.put("name", rule.getName());
            m.put("status", rule.getStatus() != null ? rule.getStatus().name() : null);
            m.put("triggerEventType", rule.getTriggerEventType());
            m.put("priority", rule.getPriority());
            m.put("effectiveAt", rule.getEffectiveAt());
            m.put("endAt", rule.getEndAt());
            return m;
        }).toList();
        Map<String, Object> bundle = new HashMap<>();
        bundle.put("tenantId", tenantId);
        bundle.put("programmeUid", programmeUid);
        bundle.put("exportedAt", LocalDateTime.now().toString());
        bundle.put("rules", rows);
        return bundle;
    }

    private static String csvCell(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
