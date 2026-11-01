package com.fragrance.ecommerce.configuration;

import com.fragrance.ecommerce.security.CaptchaVerifier;
import org.springframework.http.MediaType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
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
            var form = new LinkedMultiValueMap<String, String>();
            form.add("secret", properties.secret());
            form.add("response", token);
            var response = RestClient.create().post()
                .uri(properties.verifyUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(CaptchaResponse.class);
            if (response == null || !response.success()) {
                throw new IllegalArgumentException("CAPTCHA verification failed");
            }
        };
    }

    private record CaptchaResponse(boolean success) {}
}
