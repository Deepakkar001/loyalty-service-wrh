package com.loyaltyos.voucher.service;

import com.loyaltyos.integration.config.IntegrationProperties;
import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherCodeCryptoServiceTest {

    @Test
    void encryptDecryptRoundTrip() {
        IntegrationProperties props = new IntegrationProperties();
        props.setEncryptionKey("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        VoucherCodeCryptoService service = new VoucherCodeCryptoService(
            new IntegrationCredentialCryptoService(props)
        );

        String plain = "AMZN-TEST-1001";
        String encrypted = service.encryptCode(plain);
        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(service.decryptCode(encrypted)).isEqualTo(plain);
    }

    @Test
    void differentPlaintextProducesDifferentCiphertext() {
        IntegrationProperties props = new IntegrationProperties();
        props.setEncryptionKey("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        VoucherCodeCryptoService service = new VoucherCodeCryptoService(
            new IntegrationCredentialCryptoService(props)
        );

        assertThat(service.encryptCode("CODE-A")).isNotEqualTo(service.encryptCode("CODE-B"));
    }
}
