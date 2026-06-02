package com.loyaltyos.voucher.exception;

public class VoucherOutOfStockException extends RuntimeException {

    public VoucherOutOfStockException(String message) {
        super(message);
    }
}
