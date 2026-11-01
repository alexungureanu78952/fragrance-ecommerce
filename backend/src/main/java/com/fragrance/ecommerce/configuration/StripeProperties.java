package com.fragrance.ecommerce.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(String secretKey, String webhookSecret,
                               String successUrl, String cancelUrl) {}
