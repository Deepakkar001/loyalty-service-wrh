package com.loyaltyos.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationIpWhitelistMatcherTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void allowsWhenWhitelistEmpty() {
    assertTrue(IntegrationIpWhitelistMatcher.isAllowed(null, "192.168.1.10", objectMapper));
  }

  @Test
  void allowsMatchingIp() throws Exception {
    String json = objectMapper.writeValueAsString(java.util.List.of("192.168.1.10", "10.0.0.0/8"));
    assertTrue(IntegrationIpWhitelistMatcher.isAllowed(json, "192.168.1.10", objectMapper));
    assertTrue(IntegrationIpWhitelistMatcher.isAllowed(json, "10.1.2.3", objectMapper));
  }

  @Test
  void rejectsNonMatchingIp() throws Exception {
    String json = objectMapper.writeValueAsString(java.util.List.of("192.168.1.10"));
    assertFalse(IntegrationIpWhitelistMatcher.isAllowed(json, "203.0.113.1", objectMapper));
  }
}
