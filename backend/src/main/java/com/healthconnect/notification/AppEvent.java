package com.healthconnect.notification;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Generic event published to RabbitMQ for asynchronous, decoupled notification handling. */
public record AppEvent(
        String type,            // e.g. CALL_REQUESTED, CALL_ACCEPTED, CALL_ESCALATED, CALL_SCHEDULED
        Long recipientUserId,   // who should be notified (nullable for broadcast)
        String title,
        String message,
        Map<String, Object> data,
        Instant createdAt
) implements Serializable {
    public static AppEvent of(String type, Long recipient, String title, String message, Map<String, Object> data) {
        return new AppEvent(type, recipient, title, message, data == null ? Map.of() : data, Instant.now());
    }
}
