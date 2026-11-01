package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.security.AuthenticatedUser;
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
    public PaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CheckoutRequest request) {
        var session = payments.checkout(user.id(), request.orderId());
        return new CheckoutResponse(session.providerPaymentId(), session.checkoutUrl());
    }

    public record CheckoutRequest(@NotNull UUID orderId) {}
    public record CheckoutResponse(String providerSessionId, String checkoutUrl) {}
}
