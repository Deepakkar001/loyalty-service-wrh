package com.loyaltyos.onboarding.rules.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.onboarding.rules.config.RulesProperties;
import java.util.Objects;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Converts JSON condition trees into SpEL fragments (variables {@code #event}, {@code #customer}, {@code #tenant}, {@code #now}).
 */
@Component
public class ConditionTreeParser {

    private static final Pattern ALLOWED_FIELD = Pattern.compile("^(event|customer|tenant)(\\.[a-zA-Z0-9_]+)*$");

    private final RulesProperties rulesProperties;

    public ConditionTreeParser(RulesProperties rulesProperties) {
        this.rulesProperties = Objects.requireNonNull(rulesProperties, "rulesProperties");
    }

    public String parseConditionTree(JsonNode tree) throws ConditionParseException {
        return parseConditionTree(tree, Set.of());
    }

    /**
     * @param allowedEventPropertyNames first-level names under {@code event.*}; empty = permissive for {@code event}
     */
    public String parseConditionTree(JsonNode tree, Set<String> allowedEventPropertyNames) throws ConditionParseException {
        if (tree == null || tree.isNull() || tree.isMissingNode() || (tree.isObject() && tree.isEmpty())) {
            return "true";
        }
        int maxDepth = Math.max(4, rulesProperties.getMaxConditionDepth());
        return buildExpression(tree, 0, maxDepth, allowedEventPropertyNames != null ? allowedEventPropertyNames : Set.of());
    }

    private String buildExpression(JsonNode node, int depth, int maxDepth, Set<String> allowedEventPropertyNames)
        throws ConditionParseException {
        if (depth > maxDepth) {
            throw new ConditionParseException("Condition tree exceeds max depth (" + maxDepth + ")");
        }
        if (node == null || node.isNull()) {
            throw new ConditionParseException("Null node");
        }
        if (node.has("field") && node.get("field").isTextual()) {
            return buildLeaf(node, allowedEventPropertyNames);
        }
        String op = logicalOp(node);
        if (op == null) {
            throw new ConditionParseException("Missing logical operator or field");
        }
        String upper = op.toUpperCase(Locale.ROOT);
        if ("NOT".equals(upper)) {
            JsonNode child = node.get("node");
            if (child == null || child.isNull()) {
                throw new ConditionParseException("NOT requires 'node'");
            }
            return "!(" + buildExpression(child, depth + 1, maxDepth, allowedEventPropertyNames) + ")";
        }
        if ("AND".equals(upper) || "OR".equals(upper)) {
            JsonNode nodes = node.get("nodes");
            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                throw new ConditionParseException("AND/OR requires non-empty 'nodes' array");
            }
            List<String> parts = new ArrayList<>();
            for (JsonNode child : nodes) {
                parts.add(buildExpression(child, depth + 1, maxDepth, allowedEventPropertyNames));
            }
            String joiner = "AND".equals(upper) ? " and " : " or ";
            return "(" + String.join(joiner, parts) + ")";
        }
        throw new ConditionParseException("Unknown logical operator: " + op);
    }

    private String logicalOp(JsonNode node) {
        if (node.has("operator") && node.get("operator").isTextual()) {
            return node.get("operator").asText();
        }
        if (node.has("op") && node.get("op").isTextual()) {
            return node.get("op").asText();
        }
        return null;
    }

    private String comparisonOp(JsonNode node) {
        if (node.has("operator") && node.get("operator").isTextual()) {
            return node.get("operator").asText();
        }
        if (node.has("op") && node.get("op").isTextual()) {
            return node.get("op").asText();
        }
        return null;
    }

    private String buildLeaf(JsonNode node, Set<String> allowedEventPropertyNames) throws ConditionParseException {
        String field = node.get("field").asText().trim();
        if (!ALLOWED_FIELD.matcher(field).matches()) {
            throw new ConditionParseException("Disallowed field path: " + field);
        }
        validateEventFieldAllowlist(field, allowedEventPropertyNames);
        String spelRef = toSpelVarPath(field);
        String cmp = comparisonOp(node);
        if (cmp == null) {
            throw new ConditionParseException("Leaf requires comparison operator");
        }
        JsonNode valueNode = node.get("value");
        if (valueNode == null || valueNode.isNull()) {
            if (!"IS_NULL".equalsIgnoreCase(cmp) && !"IS_NOT_NULL".equalsIgnoreCase(cmp)) {
                throw new ConditionParseException("Leaf requires value");
            }
        }
        return switch (cmp.toUpperCase(Locale.ROOT)) {
            case "EQ" -> spelRef + " == " + literal(valueNode);
            case "NEQ" -> spelRef + " != " + literal(valueNode);
            case "GT" -> spelRef + " > " + literal(valueNode);
            case "GTE" -> spelRef + " >= " + literal(valueNode);
            case "LT" -> spelRef + " < " + literal(valueNode);
            case "LTE" -> spelRef + " <= " + literal(valueNode);
            case "IN" -> spelRef + " in " + inlineList(valueNode);
            case "NOT_IN" -> "!(" + spelRef + " in " + inlineList(valueNode) + ")";
            case "CONTAINS" -> "(" + spelRef + " != null and " + spelRef + ".contains(" + literal(valueNode) + "))";
            case "STARTS_WITH" -> "(" + spelRef + " != null and " + spelRef + ".startsWith(" + literal(valueNode) + "))";
            case "BETWEEN" -> betweenExpr(spelRef, valueNode);
            case "IS_NULL" -> spelRef + " == null";
            case "IS_NOT_NULL" -> spelRef + " != null";
            default -> throw new ConditionParseException("Unknown comparison operator: " + cmp);
        };
    }

    private static void validateEventFieldAllowlist(String field, Set<String> allowedEventPropertyNames)
        throws ConditionParseException {
        if (allowedEventPropertyNames == null || allowedEventPropertyNames.isEmpty()) {
            return;
        }
        if (!field.startsWith("event.")) {
            return;
        }
        String[] parts = field.split("\\.", 3);
        if (parts.length < 2) {
            return;
        }
        String first = parts[1];
        if (!allowedEventPropertyNames.contains(first)) {
            throw new ConditionParseException(
                "event field '" + first + "' is not declared in programme eventSchema (allowed: " + allowedEventPropertyNames + ")"
            );
        }
    }

    private String betweenExpr(String spelRef, JsonNode valueNode) throws ConditionParseException {
        if (valueNode == null || !valueNode.isArray() || valueNode.size() != 2) {
            throw new ConditionParseException("BETWEEN requires value array [low, high]");
        }
        return "(" + spelRef + " >= " + literal(valueNode.get(0)) + " and " + spelRef + " <= " + literal(valueNode.get(1)) + ")";
    }

    private String toSpelVarPath(String field) throws ConditionParseException {
        String[] parts = field.split("\\.");
        if (parts.length < 2) {
            throw new ConditionParseException("Field must be namespaced (event.*, customer.*, tenant.*): " + field);
        }
        String root = parts[0];
        if (!root.equals("event") && !root.equals("customer") && !root.equals("tenant")) {
            throw new ConditionParseException("Invalid root: " + root);
        }
        StringBuilder sb = new StringBuilder("#").append(root);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].matches("[a-zA-Z0-9_]+")) {
                throw new ConditionParseException("Invalid path segment: " + parts[i]);
            }
            sb.append("['").append(parts[i]).append("']");
        }
        return sb.toString();
    }

    private String literal(JsonNode valueNode) throws ConditionParseException {
        if (valueNode == null || valueNode.isNull()) {
            return "null";
        }
        if (valueNode.isTextual()) {
            String s = valueNode.asText();
            if (s.contains("'") || s.contains("\\")) {
                throw new ConditionParseException("Unsupported characters in string literal");
            }
            return "'" + s + "'";
        }
        if (valueNode.isNumber()) {
            return valueNode.asText();
        }
        if (valueNode.isBoolean()) {
            return Boolean.toString(valueNode.asBoolean());
        }
        throw new ConditionParseException("Unsupported literal type");
    }

    private String inlineList(JsonNode valueNode) throws ConditionParseException {
        if (valueNode == null || !valueNode.isArray() || valueNode.isEmpty()) {
            throw new ConditionParseException("IN/NOT_IN requires non-empty array value");
        }
        List<String> items = new ArrayList<>();
        for (JsonNode n : valueNode) {
            items.add(literal(n));
        }
        return "(" + String.join(", ", items) + ")";
    }
}
