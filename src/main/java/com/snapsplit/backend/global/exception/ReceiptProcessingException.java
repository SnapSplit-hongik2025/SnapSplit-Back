package com.snapsplit.backend.global.exception;

import org.springframework.http.HttpStatus;

public class ReceiptProcessingException extends RuntimeException {
    private final HttpStatus status;

    public ReceiptProcessingException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ReceiptProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
