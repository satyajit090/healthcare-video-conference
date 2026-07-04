package com.healthconnect.provider;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "video_providers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VideoProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;          // e.g. "Internal WebRTC", "Zoom", "Microsoft Teams"

    @Column(nullable = false)
    private String type;          // INTERNAL, ZOOM, TEAMS

    private boolean enabled = true;
    private boolean isDefault = false;
    private int priority = 100;   // lower = preferred

    // API credentials. Stored server-side; never returned in plaintext to clients.
    @Column(length = 2000)
    private String apiKey;
    @Column(length = 2000)
    private String apiSecret;

    private Integer maxParticipants;
    private Integer maxDurationMinutes;

    private String lastTestStatus;   // OK / FAILED / UNTESTED
    private Instant lastTestedAt;

    @CreationTimestamp
    private Instant createdAt;
}
