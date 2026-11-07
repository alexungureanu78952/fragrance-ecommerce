package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.security.AuthenticatedUser;
import com.fragrance.ecommerce.service.AuditLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;
    private final AuditLogService auditLogService;

    public PaymentController(PaymentService payments, AuditLogService auditLogService) {
        this.payments = payments;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CheckoutRequest request) {
        var session = payments.checkout(user.id(), request.orderId());
        auditLogService.record(user.id(), "CHECKOUT", "ORDER", request.orderId().toString(), "SUCCESS", null);
        return new CheckoutResponse(session.providerPaymentId(), session.checkoutUrl());
    }

    public record CheckoutRequest(@NotNull UUID orderId) {}
    public record CheckoutResponse(String providerSessionId, String checkoutUrl) {}
}
