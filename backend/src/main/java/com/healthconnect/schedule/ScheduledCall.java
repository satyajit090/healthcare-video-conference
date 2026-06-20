package com.healthconnect.schedule;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "scheduled_calls")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledCall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;
    private String patientName;
    private Long supportId;
    private String supportName;

    private String caseType;
    @Column(length = 2000)
    private String reason;

    @Column(nullable = false)
    private Instant scheduledAt;
    private int durationMinutes = 30;
    private String timezone;

    @Column(nullable = false, unique = true)
    private String roomId;

    @Column(nullable = false)
    private String status = "SCHEDULED"; // SCHEDULED, CANCELLED, COMPLETED

    private boolean reminded24h = false;
    private boolean reminded1h = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
