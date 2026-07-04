package com.healthconnect.call;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "call_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CallNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long callId;
    private Long authorId;
    private String authorName;
    @Column(length = 4000, nullable = false)
    private String text;
    @CreationTimestamp
    private Instant createdAt;
}
