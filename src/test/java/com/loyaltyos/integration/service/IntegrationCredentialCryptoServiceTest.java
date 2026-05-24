package com.loyaltyos.integration.service;

import com.loyaltyos.integration.config.IntegrationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationCredentialCryptoServiceTest {

    @Test
    void encryptDecrypt_roundTrip() {
        IntegrationProperties props = new IntegrationProperties();
        props.setEncryptionKey("test-integration-key-material");
        IntegrationCredentialCryptoService crypto = new IntegrationCredentialCryptoService(props);

        String plain = "5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c5b4a3f2e1d0c5b4a3f2e1d0c5b4a3f2e1d0";
        String encrypted = crypto.encrypt(plain);
        assertNotEquals(plain, encrypted);
        assertEquals(plain, crypto.decrypt(encrypted));
    }
}
