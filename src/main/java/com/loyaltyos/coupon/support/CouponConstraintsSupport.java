package com.loyaltyos.coupon.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.coupon.model.CouponConstraints;
import org.springframework.stereotype.Component;

@Component
public class CouponConstraintsSupport {

    private final ObjectMapper objectMapper;

    public CouponConstraintsSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CouponConstraints parse(String json) {
        if (json == null || json.isBlank()) {
            return new CouponConstraints();
        }
        try {
            return objectMapper.readValue(json, CouponConstraints.class);
        } catch (JsonProcessingException e) {
            return new CouponConstraints();
        }
    }

    public String serialize(CouponConstraints constraints) {
        if (constraints == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(constraints);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid coupon constraints", e);
        }
    }
}
