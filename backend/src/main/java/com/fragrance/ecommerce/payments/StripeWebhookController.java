package com.fragrance.ecommerce.payments;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {
    private final PaymentService payments;
    public StripeWebhookController(PaymentService payments) { this.payments = payments; }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature) {
        payments.processWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
