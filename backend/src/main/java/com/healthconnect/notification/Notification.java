package com.healthconnect.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
    @CreationTimestamp
    private Instant createdAt;
}
