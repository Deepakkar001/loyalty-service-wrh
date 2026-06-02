package com.loyaltyos.voucher.exception;

public class VoucherBatchUploadException extends RuntimeException {

    public VoucherBatchUploadException(String message) {
        super(message);
    }

    public VoucherBatchUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
