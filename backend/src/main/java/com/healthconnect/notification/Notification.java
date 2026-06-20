package com.healthconnect.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recipientUserId;
    private String type;
    private String title;
    @Column(length = 1000)
    private String message;
    private boolean read = false;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
