package com.loyaltyos.campaigns.support;

import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadColumnSpec;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadSpecResponse;
import java.util.List;

/**
 * Platform-defined CSV contract for campaign targeted customer uploads.
 */
public final class CampaignTargetUploadSpec {

    public static final String EXAMPLE_FILENAME = "loyaltyos-campaign-target-customers-template.csv";

    public static final List<String> STANDARD_HEADERS = List.of("customer_id");

    public static final String EXAMPLE_CSV = """
        customer_id
        cust_001
        cust_002
        """.trim();

    private CampaignTargetUploadSpec() {}

    public static CampaignTargetUploadSpecResponse build(CampaignProperties properties) {
        CampaignProperties.TargetCustomerUpload cfg = properties.getTargetCustomerUpload();
        CampaignTargetUploadSpecResponse spec = new CampaignTargetUploadSpecResponse();
        spec.setFormat("CSV (RFC 4180)");
        spec.setEncoding("UTF-8 (BOM accepted)");
        spec.setMaxFileSizeMb(cfg.getMaxFileSizeMb());
        spec.setMaxRows(cfg.getMaxRows());
        spec.setStandardHeaders(STANDARD_HEADERS);
        spec.setColumns(List.of(column(
            "customer_id",
            true,
            "string",
            "External customer identifier. Must match customerId on loyalty event processing.",
            "cust_001"
        )));
        spec.setExampleCsv(EXAMPLE_CSV);
        spec.setExampleFilename(EXAMPLE_FILENAME);
        spec.setNotes(List.of(
            "Header row must include exactly: " + String.join(", ", STANDARD_HEADERS),
            "One customer ID per row; blank rows are skipped.",
            "Duplicate IDs in the same file are counted once.",
            "IDs already on the campaign whitelist are skipped (append-only uploads).",
            "Set campaign audience to Specific Customers before or after upload."
        ));
        return spec;
    }

    private static CampaignTargetUploadColumnSpec column(
        String name,
        boolean required,
        String dataType,
        String description,
        String example
    ) {
        CampaignTargetUploadColumnSpec col = new CampaignTargetUploadColumnSpec();
        col.setName(name);
        col.setRequired(required);
        col.setDataType(dataType);
        col.setDescription(description);
        col.setExample(example);
        return col;
    }
}
