package com.loyaltyos.onboarding.rules.evaluation;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.StandardTypeLocator;

/**
 * Blocks {@code T(...)} / type references in SpEL (always fails type resolution).
 */
public class NoTypeSpelTypeLocator extends StandardTypeLocator {

    public NoTypeSpelTypeLocator() {
        super(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        throw new SpelEvaluationException(0, SpelMessage.TYPE_NOT_FOUND, typeName);
    }
}
