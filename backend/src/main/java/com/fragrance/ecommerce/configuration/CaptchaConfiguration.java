package com.fragrance.ecommerce.configuration;

import com.fragrance.ecommerce.security.CaptchaVerifier;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaConfiguration {
    @Bean
    CaptchaVerifier captchaVerifier(CaptchaProperties properties) {
        return token -> {
            if (!properties.enabled()) return;
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("CAPTCHA verification is required");
            }
            if (properties.secret() == null || properties.secret().isBlank()) {
                throw new IllegalStateException("CAPTCHA secret is not configured");
            }
            var response = RestClient.create().post()
                .uri(properties.verifyUrl())
                .body(Map.of("secret", properties.secret(), "response", token))
                .retrieve()
                .body(CaptchaResponse.class);
            if (response == null || !response.success()) {
                throw new IllegalArgumentException("CAPTCHA verification failed");
            }
        };
    }

    private record CaptchaResponse(boolean success) {}
}
