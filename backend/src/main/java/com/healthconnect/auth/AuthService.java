package com.healthconnect.auth;

import com.healthconnect.common.ApiException;
import com.healthconnect.common.Role;
import com.healthconnect.config.JwtUtil;
import com.healthconnect.user.User;
import com.healthconnect.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.badRequest("Email already registered");
        }
        if (req.role() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts cannot self-register");
        }
        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .role(req.role())
                .specialty(req.specialty())
                .language(req.language() == null || req.language().isBlank() ? "en" : req.language())
                .availability(req.role() == Role.SUPPORT ? "AVAILABLE" : "OFFLINE")
                .build();
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthDtos.AuthResponse toResponse(User user) {
        String token = jwtUtil.generate(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
