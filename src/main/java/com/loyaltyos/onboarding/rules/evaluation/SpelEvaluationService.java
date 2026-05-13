package com.loyaltyos.onboarding.rules.evaluation;

import com.loyaltyos.onboarding.rules.config.RulesProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SpelEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SpelEvaluationService.class);

    private final ExpressionParser expressionParser;
    private final RulesProperties rulesProperties;
    private final ThreadPoolTaskExecutor ruleSpelExecutor;

    /**
     * We expose SpEL variables as {@code #event}, {@code #customer}, {@code #tenant}, {@code #now}.
     * The UI uses the friendlier syntax {@code event.amount} / {@code customer.tierUid}, so we normalize
     * those into {@code #event['amount']} to keep authoring simple while staying safe.
     */
    private static final Pattern FRIENDLY_DOT_ACCESS =
        Pattern.compile("(?<!#)\\b(event|customer|tenant)\\.([a-zA-Z0-9_]+)\\b");

    public SpelEvaluationService(
        ExpressionParser expressionParser,
        RulesProperties rulesProperties,
        ThreadPoolTaskExecutor ruleSpelExecutor
    ) {
        this.expressionParser = Objects.requireNonNull(expressionParser, "expressionParser");
        this.rulesProperties = Objects.requireNonNull(rulesProperties, "rulesProperties");
        this.ruleSpelExecutor = Objects.requireNonNull(ruleSpelExecutor, "ruleSpelExecutor");
    }

    public boolean evaluateCondition(String spelFragment, Map<String, Object> event, Map<String, Object> customer,
                                     Map<String, Object> tenant, Instant now) {
        if (spelFragment == null || spelFragment.isBlank() || "true".equalsIgnoreCase(spelFragment.trim())) {
            return true;
        }
        String normalized = normalizeFriendlySyntax(spelFragment);
        rejectUnsafe(normalized);
        return runWithTimeout(() -> {
            @SuppressWarnings("null")
            StandardEvaluationContext ctx = secureContext(event, customer, tenant, now);
            @SuppressWarnings("null")
            Expression expr = expressionParser.parseExpression(normalized);
            @SuppressWarnings("null")
            Boolean b = expr.getValue(ctx, Boolean.class);
            return Boolean.TRUE.equals(b);
        }, false);
    }

    public BigDecimal evaluateFormula(String formula, Map<String, Object> event, Map<String, Object> customer,
                                      Map<String, Object> tenant, Instant now) {
        if (formula == null || formula.isBlank()) {
            return BigDecimal.ZERO;
        }
        String trimmed = normalizeFriendlySyntax(formula.trim());
        rejectUnsafe(trimmed);
        return runWithTimeout(() -> {
            @SuppressWarnings("null")
            StandardEvaluationContext ctx = secureContext(event, customer, tenant, now);
            @SuppressWarnings("null")
            Expression expr = expressionParser.parseExpression(trimmed);
            @SuppressWarnings("null")
            Number n = expr.getValue(ctx, Number.class);
            if (n == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(n.toString()).setScale(4, RoundingMode.HALF_UP);
        }, BigDecimal.ZERO);
    }

    private static String normalizeFriendlySyntax(String expr) {
        // Convert event.amount -> #event['amount'] (same for customer/tenant) unless already #event...
        Matcher m = FRIENDLY_DOT_ACCESS.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String root = m.group(1);
            String prop = m.group(2);
            m.appendReplacement(sb, "#" + root + "['" + prop + "']");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private StandardEvaluationContext secureContext(Map<String, Object> event, Map<String, Object> customer,
                                                    Map<String, Object> tenant, Instant now) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setTypeLocator(new NoTypeSpelTypeLocator());
        ctx.setVariable("event", event);
        ctx.setVariable("customer", customer);
        ctx.setVariable("tenant", tenant);
        ctx.setVariable("now", now);
        return ctx;
    }

    private <T> T runWithTimeout(Callable<T> action, T onFailure) {
        int ms = rulesProperties.getEvaluationTimeoutMs();
        if (ms <= 0) {
            try {
                return action.call();
            } catch (Exception e) {
                log.warn("SpEL evaluation failed", e);
                return onFailure;
            }
        }
        @SuppressWarnings("null")
        Future<T> f = ruleSpelExecutor.submit(action);
        try {
            return f.get(ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            log.warn("SpEL evaluation timed out after {} ms", ms);
            return onFailure;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            f.cancel(true);
            return onFailure;
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof SpelEvaluationException || c instanceof ParseException) {
                log.warn("SpEL evaluation failed: {}", c.getMessage());
            } else {
                log.warn("SpEL evaluation failed", c);
            }
            return onFailure;
        }
    }

    private void rejectUnsafe(String expr) {
        String upper = expr.toUpperCase();
        if (upper.contains("T(")) {
            throw new IllegalArgumentException("Disallowed SpEL: type references");
        }
        if (expr.contains("@")) {
            throw new IllegalArgumentException("Disallowed SpEL: bean references");
        }
    }
}
