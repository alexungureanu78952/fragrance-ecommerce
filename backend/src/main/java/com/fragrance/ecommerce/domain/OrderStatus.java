package com.fragrance.ecommerce.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELED
}
