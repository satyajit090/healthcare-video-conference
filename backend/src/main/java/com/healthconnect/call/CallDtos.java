package com.healthconnect.call;

import com.healthconnect.common.CallStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class CallDtos {

    public record StartCallRequest(
            String caseType,
            @NotBlank String reason,
            boolean recordingConsent
    ) {}

    public record RateRequest(int rating, String comment) {}

    public record NoteRequest(@NotBlank String text) {}

    public record EscalateRequest(@NotBlank String reason) {}

    public record CallView(
            Long id, String roomId, Long patientId, String patientName, String patientLanguage,
            Long supportId, String supportName, String caseType, String reason,
            CallStatus status, boolean scheduled, boolean urgent, boolean recordingConsent,
            String escalationReason, Integer rating, String ratingComment, String aiTriageSummary,
            Instant createdAt, Instant startedAt, Instant endedAt, Long durationSeconds
    ) {
        public static CallView of(CallSession c) {
            return new CallView(c.getId(), c.getRoomId(), c.getPatientId(), c.getPatientName(),
                    c.getPatientLanguage(), c.getSupportId(), c.getSupportName(), c.getCaseType(),
                    c.getReason(), c.getStatus(), c.isScheduled(), c.isUrgent(), c.isRecordingConsent(),
                    c.getEscalationReason(), c.getRating(), c.getRatingComment(), c.getAiTriageSummary(),
                    c.getCreatedAt(), c.getStartedAt(), c.getEndedAt(), c.getDurationSeconds());
        }
    }

    public record NoteView(Long id, Long authorId, String authorName, String text, Instant createdAt) {
        public static NoteView of(CallNote n) {
            return new NoteView(n.getId(), n.getAuthorId(), n.getAuthorName(), n.getText(), n.getCreatedAt());
        }
    }

    public record CallDetail(CallView call, List<NoteView> notes) {}

    // WebRTC signaling envelope relayed verbatim between the two peers in a room.
    public record SignalMessage(String type, Long fromUserId, String fromName, Object payload) {}

    // In-call chat message.
    public record ChatMessage(Long fromUserId, String fromName, String text, Instant sentAt) {}
}
