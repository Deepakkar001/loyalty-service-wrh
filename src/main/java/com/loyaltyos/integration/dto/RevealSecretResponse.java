package com.loyaltyos.integration.dto;

import java.time.Instant;

public class RevealSecretResponse {

    private String keyId;
    private String apiKey;
    private String signingSecret;
    private Instant lastSecretRevealedAt;
    private String warning;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSigningSecret() { return signingSecret; }
    public void setSigningSecret(String signingSecret) { this.signingSecret = signingSecret; }
    public Instant getLastSecretRevealedAt() { return lastSecretRevealedAt; }
    public void setLastSecretRevealedAt(Instant lastSecretRevealedAt) { this.lastSecretRevealedAt = lastSecretRevealedAt; }
    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
