package com.loyaltyos.onboarding.analytics.controller;

import com.loyaltyos.onboarding.analytics.service.ExportService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/analytics/export")
@Tag(name = "Analytics Export", description = "Streaming CSV and JSON exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = Objects.requireNonNull(exportService, "exportService");
    }

    @GetMapping(value = "/points-ledger", produces = "text/csv")
    @Operation(summary = "Export points ledger as CSV")
    public ResponseEntity<StreamingResponseBody> exportPointsLedger(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "points-ledger-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "id,customer_id,entry_type,points,source_rule_id,created_at\n",
            writer -> exportService.streamPointsLedger(tenantId, programmeUid, from, to, row -> writeLine(writer, row))
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/rule-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Export earn rule configuration bundle")
    public ResponseEntity<Map<String, Object>> exportRuleConfig(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            exportService.exportRuleConfigBundle(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping(value = "/webhook-log", produces = "text/csv")
    @Operation(summary = "Export webhook delivery log as CSV")
    public ResponseEntity<StreamingResponseBody> exportWebhookLog(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "webhook-log-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "id,event_type,status,attempt_count,last_error,created_at\n",
            writer -> exportService.streamWebhookLog(tenantId, from, to, row -> writeLine(writer, row))
        );
        return csvAttachment(filename, body);
    }

    private static ResponseEntity<StreamingResponseBody> csvAttachment(String filename, StreamingResponseBody body) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body);
    }

    @FunctionalInterface
    private interface CsvStreamAction {
        void run(OutputStreamWriter writer) throws IOException;
    }

    private static void streamCsv(java.io.OutputStream out, String header, CsvStreamAction action) throws IOException {
        var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(header);
        action.run(writer);
        writer.flush();
    }

    private static void writeLine(OutputStreamWriter writer, String line) {
        try {
            writer.write(line);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
