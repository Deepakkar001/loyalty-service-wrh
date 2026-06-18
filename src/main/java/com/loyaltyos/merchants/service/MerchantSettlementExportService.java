package com.loyaltyos.merchants.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.loyaltyos.merchants.dto.SettlementLineItemResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantSettlementCycle;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.repository.MerchantSettlementCycleRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantSettlementExportService {

    private final MerchantRepository merchantRepository;
    private final MerchantSettlementCycleRepository cycleRepository;
    private final MerchantSettlementService settlementService;

    public MerchantSettlementExportService(
        MerchantRepository merchantRepository,
        MerchantSettlementCycleRepository cycleRepository,
        MerchantSettlementService settlementService
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.cycleRepository = Objects.requireNonNull(cycleRepository, "cycleRepository");
        this.settlementService = Objects.requireNonNull(settlementService, "settlementService");
    }

    @Transactional(readOnly = true)
    public byte[] export(String tenantId, String merchantUid, String cycleUid, String format) {
        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
        MerchantSettlementCycle cycle = cycleRepository.findByTenantIdAndCycleUid(tenantId, cycleUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement cycle not found"));
        if (!merchantUid.equals(cycle.getMerchantUid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Settlement cycle does not belong to merchant");
        }
        List<SettlementLineItemResponse> lines = settlementService.listLineItems(tenantId, merchantUid, cycleUid);
        String normalized = format == null ? "csv" : format.trim().toLowerCase();
        return switch (normalized) {
            case "pdf" -> exportPdf(merchant, cycle, lines);
            case "xlsx", "excel" -> exportExcel(merchant, cycle, lines);
            case "csv" -> exportCsv(merchant, cycle, lines);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
        };
    }

    private byte[] exportCsv(Merchant merchant, MerchantSettlementCycle cycle, List<SettlementLineItemResponse> lines) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(
                 new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                 CSVFormat.DEFAULT.builder().setHeader(
                     "merchantUid", "legalName", "cycleUid", "periodStart", "periodEnd", "status",
                     "lineItemUid", "txnReference", "pointsAmount", "monetaryValue", "disputed"
                 ).build()
             )) {
            for (SettlementLineItemResponse line : lines) {
                printer.printRecord(
                    merchant.getMerchantUid(),
                    merchant.getLegalName(),
                    cycle.getCycleUid(),
                    cycle.getPeriodStart(),
                    cycle.getPeriodEnd(),
                    cycle.getStatus().name(),
                    line.getLineItemUid(),
                    line.getTxnReference(),
                    line.getPointsAmount(),
                    line.getMonetaryValue(),
                    line.isDisputed()
                );
            }
            printer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export CSV statement");
        }
    }

    private byte[] exportExcel(
        Merchant merchant,
        MerchantSettlementCycle cycle,
        List<SettlementLineItemResponse> lines
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Settlement");
            Row header = sheet.createRow(0);
            String[] cols = {
                "merchantUid", "legalName", "cycleUid", "periodStart", "periodEnd", "status",
                "lineItemUid", "txnReference", "pointsAmount", "monetaryValue", "disputed"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            int rowIdx = 1;
            for (SettlementLineItemResponse line : lines) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(merchant.getMerchantUid());
                row.createCell(1).setCellValue(merchant.getLegalName());
                row.createCell(2).setCellValue(cycle.getCycleUid());
                row.createCell(3).setCellValue(cycle.getPeriodStart().toString());
                row.createCell(4).setCellValue(cycle.getPeriodEnd().toString());
                row.createCell(5).setCellValue(cycle.getStatus().name());
                row.createCell(6).setCellValue(line.getLineItemUid());
                row.createCell(7).setCellValue(line.getTxnReference());
                row.createCell(8).setCellValue(line.getPointsAmount());
                row.createCell(9).setCellValue(toDouble(line.getMonetaryValue()));
                row.createCell(10).setCellValue(line.isDisputed());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export Excel statement");
        }
    }

    private byte[] exportPdf(Merchant merchant, MerchantSettlementCycle cycle, List<SettlementLineItemResponse> lines) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            document.add(new Paragraph("Merchant Settlement Statement", title));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Merchant: " + merchant.getLegalName() + " (" + merchant.getMerchantUid() + ")", body));
            document.add(new Paragraph(
                "Period: " + cycle.getPeriodStart() + " to " + cycle.getPeriodEnd(), body));
            document.add(new Paragraph("Status: " + cycle.getStatus().name(), body));
            document.add(new Paragraph(
                "Total: " + cycle.getTotalMonetaryValue() + " (" + cycle.getTotalPoints() + " points)", body));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Line items", title));
            for (SettlementLineItemResponse line : lines) {
                document.add(new Paragraph(
                    "- " + line.getTxnReference()
                        + " | points=" + line.getPointsAmount()
                        + " | value=" + line.getMonetaryValue()
                        + (line.isDisputed() ? " | DISPUTED" : ""),
                    body
                ));
            }
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export PDF statement");
        }
    }

    private static double toDouble(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }
}
