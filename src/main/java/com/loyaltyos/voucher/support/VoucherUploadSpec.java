package com.loyaltyos.voucher.support;

import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherUploadColumnSpec;
import com.loyaltyos.voucher.dto.VoucherUploadSpecResponse;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for voucher CSV upload format (portal + docs + import parser).
 * <p>
 * v1 contract: fixed header columns ({@link #STANDARD_HEADERS}); mandatory cell values for
 * code, face_value, currency, expires_at. PIN and partner_sku are optional values.
 */
public final class VoucherUploadSpec {

    public static final String EXAMPLE_FILENAME = "loyaltyos-voucher-codes-template.csv";

    /**
     * Every upload must include this header row (exact names, case-sensitive). Tenants may not
     * omit or rename these columns; optional fields are left empty when not applicable.
     */
    public static final List<String> STANDARD_HEADERS = List.of(
        "code",
        "pin",
        "face_value",
        "currency",
        "expires_at",
        "partner_sku"
    );

    /** Columns that must have a non-empty, valid value on every data row. */
    public static final List<String> MANDATORY_VALUE_COLUMNS = List.of(
        "code",
        "face_value",
        "currency",
        "expires_at"
    );

    /** Extra columns accepted if present; not part of the downloadable template. */
    public static final Set<String> EXTENDED_OPTIONAL_HEADERS = Set.of(
        "serial",
        "region",
        "channel",
        "external_ref"
    );

    /**
     * Example file: all standard headers, both rows satisfy mandatory fields; row 2 shows empty PIN.
     */
    public static final String EXAMPLE_CSV = """
        code,pin,face_value,currency,expires_at,partner_sku
        AMZN-XXXX-1001,1234,500.00,INR,2026-12-31T23:59:59Z,AMZ-500
        AMZN-XXXX-1002,,250.00,INR,2026-12-31T23:59:59Z,AMZ-250
        """.trim();

    private VoucherUploadSpec() {}

    public static VoucherUploadSpecResponse build(VoucherProperties properties) {
        VoucherUploadSpecResponse spec = new VoucherUploadSpecResponse();
        spec.setFormat("CSV (RFC 4180)");
        spec.setEncoding("UTF-8 (BOM accepted)");
        spec.setMaxFileSizeMb(properties.getMaxFileSizeMb());
        spec.setMaxRows(properties.getMaxRows());
        spec.setStandardHeaders(STANDARD_HEADERS);
        spec.setColumns(List.of(
            column(
                "code",
                true,
                "string",
                "Partner voucher code. Unique per tenant; stored encrypted. Trimmed and uppercased on import.",
                "AMZN-XXXX-1001"
            ),
            column(
                "pin",
                false,
                "string",
                "Optional PIN or secondary secret. Leave empty if not used. Max 128 characters.",
                "1234"
            ),
            column(
                "face_value",
                true,
                "decimal",
                "Nominal monetary value from the partner (not loyalty points). Shown on issue API response.",
                "500.00"
            ),
            column(
                "currency",
                true,
                "ISO 4217",
                "Currency for face_value (3 letters). Required with face_value.",
                "INR"
            ),
            column(
                "expires_at",
                true,
                "datetime",
                "Code validity end in UTC (ISO-8601, e.g. 2026-12-31T23:59:59Z). Must be after import time.",
                "2026-12-31T23:59:59Z"
            ),
            column(
                "partner_sku",
                false,
                "string",
                "Partner product or SKU reference for reconciliation. May be left empty.",
                "AMZ-500"
            )
        ));
        spec.setExampleCsv(EXAMPLE_CSV);
        spec.setExampleFilename(EXAMPLE_FILENAME);
        spec.setNotes(List.of(
            "Header row is fixed: " + String.join(", ", STANDARD_HEADERS) + " — do not remove or rename columns.",
            "Mandatory on every data row: code, face_value, currency, expires_at.",
            "Optional values: pin, partner_sku (column must still be present; leave cell empty if unused).",
            "Points cost is configured in Rewards Catalog, not in this file.",
            "Programme and catalog reward are selected in the portal before upload.",
            "Codes must be unique in your tenant inventory; duplicates are skipped."
        ));
        return spec;
    }

    private static VoucherUploadColumnSpec column(
        String name,
        boolean required,
        String dataType,
        String description,
        String example
    ) {
        VoucherUploadColumnSpec col = new VoucherUploadColumnSpec();
        col.setName(name);
        col.setRequired(required);
        col.setDataType(dataType);
        col.setDescription(description);
        col.setExample(example);
        return col;
    }
}
