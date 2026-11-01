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
        @NotBlank @Size(max = 120) String displayName
    ) {
    }

    public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password
    ) {
    }

    public record UserResponse(UUID id, String email, String displayName, Set<String> roles) {
    }
}
