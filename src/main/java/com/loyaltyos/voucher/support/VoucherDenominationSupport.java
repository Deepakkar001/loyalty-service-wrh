package com.loyaltyos.voucher.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VoucherDenominationSupport {

    private VoucherDenominationSupport() {}

    public static BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.UNNECESSARY).stripTrailingZeros();
    }

    public static boolean amountsEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return normalizeAmount(a).compareTo(normalizeAmount(b)) == 0;
    }
}
