package com.fragrance.ecommerce.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 120) String displayName,
        String captchaToken
    ) {
        public RegisterRequest(String email, String password, String displayName) {
            this(email, password, displayName, null);
        }
    }

    public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password,
        String captchaToken
    ) {
        public LoginRequest(String email, String password) {
            this(email, password, null);
        }
    }

    public record UserResponse(UUID id, String email, String displayName, Set<String> roles) {
    }
}
