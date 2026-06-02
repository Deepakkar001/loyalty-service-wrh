package com.loyaltyos.voucher.service;

import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import org.springframework.stereotype.Service;

/**
 * Encrypts voucher codes at import; decrypts only when issuing to a customer.
 */
@Service
public class VoucherCodeCryptoService {

    private final IntegrationCredentialCryptoService cryptoService;

    public VoucherCodeCryptoService(IntegrationCredentialCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public String encryptCode(String plaintext) {
        return cryptoService.encrypt(plaintext);
    }

    public String decryptCode(String ciphertext) {
        return cryptoService.decrypt(ciphertext);
    }
}
