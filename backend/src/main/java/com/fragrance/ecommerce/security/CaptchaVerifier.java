package com.fragrance.ecommerce.security;

public interface CaptchaVerifier {
    void verify(String token);
}
