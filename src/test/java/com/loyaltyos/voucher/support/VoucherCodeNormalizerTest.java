package com.loyaltyos.voucher.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherCodeNormalizerTest {

    @Test
    void normalizeTrimsAndUppercases() {
        assertThat(VoucherCodeNormalizer.normalize("  amzn-x  ")).isEqualTo("AMZN-X");
    }

    @Test
    void sha256HexIsDeterministic() {
        String a = VoucherCodeNormalizer.sha256Hex("AMZN-X");
        String b = VoucherCodeNormalizer.sha256Hex("AMZN-X");
        assertThat(a).isEqualTo(b).hasSize(64);
    }
}
