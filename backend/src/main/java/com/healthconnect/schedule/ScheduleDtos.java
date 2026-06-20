package com.healthconnect.schedule;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ScheduleDtos {
    public record BookRequest(
            Long supportId, String caseType, String reason,
            @NotNull Instant scheduledAt, Integer durationMinutes, String timezone) {}

    public record View(Long id, Long patientId, String patientName, Long supportId, String supportName,
                       String caseType, String reason, Instant scheduledAt, int durationMinutes,
                       String timezone, String roomId, String status) {
        public static View of(ScheduledCall s) {
            return new View(s.getId(), s.getPatientId(), s.getPatientName(), s.getSupportId(), s.getSupportName(),
                    s.getCaseType(), s.getReason(), s.getScheduledAt(), s.getDurationMinutes(),
                    s.getTimezone(), s.getRoomId(), s.getStatus());
        }
    }
}
