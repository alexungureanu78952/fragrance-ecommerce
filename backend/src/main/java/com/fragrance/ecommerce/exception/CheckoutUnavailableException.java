package com.fragrance.ecommerce.exception;

public class CheckoutUnavailableException extends RuntimeException {
    public CheckoutUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
