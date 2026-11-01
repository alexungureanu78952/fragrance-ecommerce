package com.fragrance.ecommerce.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.captcha")
public record CaptchaProperties(boolean enabled, String secret, String verifyUrl) {}
