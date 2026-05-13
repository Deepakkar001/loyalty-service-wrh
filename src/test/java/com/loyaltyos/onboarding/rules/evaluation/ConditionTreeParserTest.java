package com.loyaltyos.onboarding.rules.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rules.config.RulesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionTreeParserTest {

    private ConditionTreeParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RulesProperties props = new RulesProperties();
        props.setMaxConditionDepth(64);
        parser = new ConditionTreeParser(props);
        objectMapper = new ObjectMapper();
    }

    @Test
    void emptyTree_isTrue() throws Exception {
        assertEquals("true", parser.parseConditionTree(objectMapper.readTree("null")));
        assertEquals("true", parser.parseConditionTree(objectMapper.readTree("{}")));
    }

    @Test
    void leaf_gte() throws Exception {
        var tree = objectMapper.readTree("""
            {"field":"event.amount","operator":"GTE","value":500}
            """);
        String s = parser.parseConditionTree(tree, Set.of());
        assertTrue(s.contains("#event['amount']"));
        assertTrue(s.contains(">="));
        assertTrue(s.contains("500"));
    }

    @Test
    void eventFieldAllowlist_blocksUnknown() throws Exception {
        var tree = objectMapper.readTree("""
            {"field":"event.unknownField","operator":"EQ","value":1}
            """);
        assertThrows(ConditionParseException.class, () ->
            parser.parseConditionTree(tree, Set.of("amount")));
    }

    @Test
    void eventFieldAllowlist_allowsDeclared() throws Exception {
        var tree = objectMapper.readTree("""
            {"field":"event.amount","operator":"GTE","value":1}
            """);
        String s = parser.parseConditionTree(tree, Set.of("amount"));
        assertTrue(s.contains("#event['amount']"));
    }

    @Test
    void and_nodes() throws Exception {
        var tree = objectMapper.readTree("""
            {"op":"AND","nodes":[
              {"field":"event.amount","op":"GTE","value":500},
              {"field":"customer.tierUid","op":"EQ","value":"gold"}
            ]}
            """);
        String s = parser.parseConditionTree(tree, Set.of());
        assertTrue(s.contains(" and "));
        assertTrue(s.contains("#customer['tierUid']"));
    }

    @Test
    void not_node() throws Exception {
        var tree = objectMapper.readTree("""
            {"op":"NOT","node":{"field":"event.eventType","op":"EQ","value":"REFUND"}}
            """);
        String s = parser.parseConditionTree(tree, Set.of());
        assertTrue(s.startsWith("!("));
    }

    @Test
    void in_requiresNonEmptyArray() throws Exception {
        var tree = objectMapper.readTree("""
            {"field":"event.channel","op":"IN","value":[]}
            """);
        assertThrows(ConditionParseException.class, () -> parser.parseConditionTree(tree, Set.of()));
    }

    @Test
    void between_requiresTwoValues() throws Exception {
        var tree = objectMapper.readTree("""
            {"field":"event.amount","op":"BETWEEN","value":[1]}
            """);
        assertThrows(ConditionParseException.class, () -> parser.parseConditionTree(tree, Set.of()));
    }

    @Test
    void maxDepth_isEnforced() throws Exception {
        RulesProperties props = new RulesProperties();
        props.setMaxConditionDepth(4);
        ConditionTreeParser smallDepthParser = new ConditionTreeParser(props);

        var tree = objectMapper.readTree("""
            {"op":"NOT","node":{"op":"NOT","node":{"op":"NOT","node":{"op":"NOT","node":{"op":"NOT","node":{"op":"NOT","node":{"field":"event.amount","op":"GTE","value":1}}}}}}}
            """);
        assertThrows(ConditionParseException.class, () -> smallDepthParser.parseConditionTree(tree, Set.of()));
    }
}
