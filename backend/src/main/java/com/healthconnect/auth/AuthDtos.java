package com.healthconnect.auth;

import com.healthconnect.common.Role;
import jakarta.validation.constraints.*;

public class AuthDtos {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotBlank String fullName,
            @NotNull Role role,
            String specialty,
            String language
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String fullName,
            String email,
            Role role
    ) {}
}
