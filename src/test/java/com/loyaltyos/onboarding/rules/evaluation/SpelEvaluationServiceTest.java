package com.loyaltyos.onboarding.rules.evaluation;

import com.loyaltyos.onboarding.rules.config.RulesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpelEvaluationServiceTest {

    private SpelEvaluationService spel;

    @BeforeEach
    void setUp() {
        RulesProperties props = new RulesProperties();
        props.setEvaluationTimeoutMs(0);
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.initialize();
        spel = new SpelEvaluationService(new SpelExpressionParser(), props, ex);
    }

    @Test
    void condition_trueLiteral() {
        assertTrue(spel.evaluateCondition("true", Map.of(), Map.of(), Map.of(), Instant.now()));
    }

    @Test
    void condition_numericCompare() {
        Map<String, Object> event = Map.of("amount", new BigDecimal("600"));
        assertTrue(spel.evaluateCondition("#event['amount'] >= 500", event, Map.of(), Map.of(), Instant.now()));
    }

    @Test
    void formula_multiply() {
        Map<String, Object> event = Map.of("amount", new BigDecimal("100"));
        Map<String, Object> tenant = Map.of("basePointsRate", new BigDecimal("0.1"));
        BigDecimal v = spel.evaluateFormula("#event['amount'] * #tenant['basePointsRate']", event, Map.of(), tenant, Instant.now());
        assertEquals(0, v.compareTo(new BigDecimal("10.0000")));
    }

    @Test
    void rejectsTypeReferenceInSource() {
        assertThrows(IllegalArgumentException.class, () ->
            spel.evaluateFormula("T(java.lang.Runtime).getRuntime().availableProcessors()", Map.of(), Map.of(), Map.of(), Instant.now()));
    }
}
