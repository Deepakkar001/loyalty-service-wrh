package com.loyaltyos.voucher.support;

import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherUploadSpecResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherUploadSpecTest {

    @Test
    void exampleCsvContainsAllStandardHeadersAndMandatorySampleValues() {
        VoucherProperties props = new VoucherProperties();
        VoucherUploadSpecResponse spec = VoucherUploadSpec.build(props);

        String[] headerLine = spec.getExampleCsv().lines().findFirst().orElse("").split(",");
        assertThat(headerLine).containsExactlyElementsOf(VoucherUploadSpec.STANDARD_HEADERS);

        long mandatoryColumns = spec.getColumns().stream().filter(c -> c.isRequired()).count();
        assertThat(mandatoryColumns).isEqualTo(4);

        assertThat(spec.getColumns().stream().filter(c -> c.isRequired()).map(c -> c.getName()))
            .containsExactly("code", "face_value", "currency", "expires_at");
    }
}
