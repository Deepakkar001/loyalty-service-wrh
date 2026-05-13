package com.loyaltyos.onboarding.rules.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.onboarding.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.onboarding.rules.service.RuleEvaluationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RuleEvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RuleEvaluationControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleEvaluationService ruleEvaluationService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor tenantJwt(String tenantId) {
        return request -> {
            Jwt jwt = Jwt.withTokenValue("test")
                .headers(h -> h.put("alg", "none"))
                .claim("tenantId", tenantId)
                .claim("role", "TENANT")
                .build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
            return request;
        };
    }

    @Test
    void evaluate_returnsOk() throws Exception {
        when(ruleEvaluationService.evaluate(eq("t1"), any())).thenReturn(
            RuleEvaluationResponse.builder()
                .tenantId("t1")
                .success(true)
                .finalPointsAwarded(BigDecimal.ONE)
                .build()
        );

        RuleEvaluateRequest req = RuleEvaluateRequest.builder()
            .customerId("c1")
            .eventId("e1")
            .eventType("PURCHASE")
            .amount(new BigDecimal("10"))
            .eventPayload(objectMapper.createObjectNode())
            .build();

        mockMvc.perform(post("/api/v1/engine/rule/evaluate")
                .with(tenantJwt("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
