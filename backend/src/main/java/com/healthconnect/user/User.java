package com.healthconnect.user;

import com.healthconnect.common.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // For SUPPORT staff: available / busy / offline
    @Column(nullable = false)
    private String availability = "OFFLINE";

    // case type / specialty (e.g. "General", "Yoga", "Cardiology")
    private String specialty;

    // preferred language, used for matching (User Story: multi-language)
    @Column(nullable = false)
    private String language = "en";

    private boolean senior = false; // senior staff for escalation

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
