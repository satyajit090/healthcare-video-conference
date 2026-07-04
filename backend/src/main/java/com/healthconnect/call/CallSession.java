package com.healthconnect.call;

import com.healthconnect.common.CallStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "call_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CallSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomId;            // WebRTC room identifier

    @Column(nullable = false)
    private Long patientId;
    private String patientName;
    private String patientLanguage;

    private Long supportId;           // null until accepted
    private String supportName;

    private String caseType;          // requested specialty
    @Column(length = 2000)
    private String reason;            // patient's described concern (also fed to AI triage)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status = CallStatus.REQUESTED;

    private boolean scheduled = false;
    private boolean urgent = false;
    private boolean recordingConsent = false;

    @Column(length = 2000)
    private String escalationReason;
    private Long escalatedToId;

    private Integer rating;            // 1..5
    @Column(length = 1000)
    private String ratingComment;

    @Column(length = 4000)
    private String aiTriageSummary;    // Gemini-generated triage note

    @CreationTimestamp
    private Instant createdAt;
    private Instant startedAt;
    private Instant endedAt;
    private Long durationSeconds;
}
