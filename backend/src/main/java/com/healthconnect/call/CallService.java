package com.healthconnect.call;

import com.healthconnect.ai.GeminiService;
import com.healthconnect.common.ApiException;
import com.healthconnect.common.CallStatus;
import com.healthconnect.common.Role;
import com.healthconnect.notification.AppEvent;
import com.healthconnect.notification.EventPublisher;
import com.healthconnect.user.User;
import com.healthconnect.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CallService {

    private final CallRepository callRepository;
    private final CallNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final EventPublisher events;
    private final GeminiService gemini;

    public CallService(CallRepository callRepository, CallNoteRepository noteRepository,
                       UserRepository userRepository, EventPublisher events, GeminiService gemini) {
        this.callRepository = callRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.gemini = gemini;
    }

    private User require(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional
    public CallSession startInstantCall(Long patientId, CallDtos.StartCallRequest req) {
        User patient = require(patientId);
        if (patient.getRole() != Role.PATIENT) throw ApiException.forbidden("Only patients can start support calls");

        CallSession call = CallSession.builder()
                .roomId(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .patientName(patient.getFullName())
                .patientLanguage(patient.getLanguage())
                .caseType(req.caseType())
                .reason(req.reason())
                .recordingConsent(req.recordingConsent())
                .status(CallStatus.REQUESTED)
                .build();

        // AI triage summary for the support person (best-effort, non-blocking on failure).
        String triage = gemini.generate(
                "You are a clinical intake assistant for a healthcare & yoga support platform. "
              + "Given a patient's described concern, produce a concise triage note (max 4 short lines): "
              + "likely category, urgency (low/medium/high), and 2 suggested questions for the support agent. "
              + "Never give a diagnosis or treatment plan.",
                "Patient concern: " + req.reason()
              + (req.caseType() != null ? "\nRequested area: " + req.caseType() : ""));
        call.setAiTriageSummary(triage);

        call = callRepository.save(call);

        // Notify available support staff (broadcast to matching-language staff, fallback all).
        List<User> support = userRepository.findByRoleAndAvailability(Role.SUPPORT, "AVAILABLE");
        for (User s : support) {
            if (s.getLanguage() != null && patient.getLanguage() != null
                    && !s.getLanguage().equalsIgnoreCase(patient.getLanguage())) continue;
            events.publish(AppEvent.of("CALL_REQUESTED", s.getId(),
                    "New support call",
                    patient.getFullName() + " requested a video call" +
                            (req.caseType() != null ? " (" + req.caseType() + ")" : ""),
                    Map.of("callId", call.getId(), "roomId", call.getRoomId())));
        }
        return call;
    }

    @Transactional(readOnly = true)
    public List<CallSession> queue() {
        return callRepository.findByStatusOrderByUrgentDescCreatedAtAsc(CallStatus.REQUESTED);
    }

    @Transactional
    public CallSession accept(Long callId, Long supportId) {
        User support = require(supportId);
        if (support.getRole() != Role.SUPPORT) throw ApiException.forbidden("Only support staff can accept calls");
        CallSession call = require(callId, true);
        if (call.getStatus() != CallStatus.REQUESTED) throw ApiException.badRequest("Call is no longer available");

        call.setSupportId(support.getId());
        call.setSupportName(support.getFullName());
        call.setStatus(CallStatus.RINGING);
        support.setAvailability("BUSY");
        userRepository.save(support);

        events.publish(AppEvent.of("CALL_ACCEPTED", call.getPatientId(),
                "Support is joining", support.getFullName() + " accepted your call",
                Map.of("callId", call.getId(), "roomId", call.getRoomId())));
        return callRepository.save(call);
    }

    @Transactional
    public CallSession reject(Long callId, Long supportId) {
        CallSession call = require(callId, true);
        if (call.getStatus() != CallStatus.REQUESTED && call.getStatus() != CallStatus.RINGING) {
            throw ApiException.badRequest("Call cannot be rejected now");
        }
        call.setStatus(CallStatus.REJECTED);
        call.setEndedAt(Instant.now());
        events.publish(AppEvent.of("CALL_REJECTED", call.getPatientId(),
                "Call not accepted", "Your call request was declined. Please try again.",
                Map.of("callId", call.getId())));
        return callRepository.save(call);
    }

    @Transactional
    public CallSession cancel(Long callId, Long patientId) {
        CallSession call = require(callId, true);
        if (!call.getPatientId().equals(patientId)) throw ApiException.forbidden("Not your call");
        if (call.getStatus() != CallStatus.REQUESTED && call.getStatus() != CallStatus.RINGING) {
            throw ApiException.badRequest("Only pending calls can be cancelled");
        }
        call.setStatus(CallStatus.CANCELLED);
        call.setEndedAt(Instant.now());
        return callRepository.save(call);
    }

    @Transactional
    public CallSession markActive(Long callId, Long userId) {
        CallSession call = require(callId, true);
        assertParticipant(call, userId);
        if (call.getStartedAt() == null) call.setStartedAt(Instant.now());
        call.setStatus(CallStatus.ACTIVE);
        return callRepository.save(call);
    }

    @Transactional
    public CallSession end(Long callId, Long userId) {
        CallSession call = require(callId, true);
        assertParticipant(call, userId);
        if (call.getStatus() == CallStatus.COMPLETED) return call;
        Instant now = Instant.now();
        call.setEndedAt(now);
        if (call.getStartedAt() != null) {
            call.setDurationSeconds(Duration.between(call.getStartedAt(), now).getSeconds());
        }
        call.setStatus(CallStatus.COMPLETED);
        if (call.getSupportId() != null) {
            userRepository.findById(call.getSupportId()).ifPresent(s -> {
                s.setAvailability("AVAILABLE");
                userRepository.save(s);
            });
        }
        events.publish(AppEvent.of("CALL_COMPLETED", call.getPatientId(),
                "Call ended", "Your support session has ended. You can rate it now.",
                Map.of("callId", call.getId())));
        return callRepository.save(call);
    }

    @Transactional
    public CallSession escalate(Long callId, Long supportId, String reason) {
        CallSession call = require(callId, true);
        assertParticipant(call, supportId);
        call.setUrgent(true);
        call.setStatus(CallStatus.ESCALATED);
        call.setEscalationReason(reason);
        callRepository.save(call);
        for (User senior : userRepository.findByRoleAndSeniorTrue(Role.SUPPORT)) {
            events.publish(AppEvent.of("CALL_ESCALATED", senior.getId(),
                    "URGENT: escalation",
                    "Case escalated by " + call.getSupportName() + ": " + reason,
                    Map.of("callId", call.getId(), "roomId", call.getRoomId())));
        }
        events.publish(AppEvent.of("CALL_ESCALATED_PATIENT", call.getPatientId(),
                "Bringing in a specialist",
                "A senior specialist is being invited to your call to assist further.",
                Map.of("callId", call.getId())));
        return call;
    }

    @Transactional
    public CallSession rate(Long callId, Long patientId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw ApiException.badRequest("Rating must be 1-5");
        CallSession call = require(callId, true);
        if (!call.getPatientId().equals(patientId)) throw ApiException.forbidden("Not your call");
        call.setRating(rating);
        call.setRatingComment(comment);
        return callRepository.save(call);
    }

    @Transactional
    public CallNote addNote(Long callId, Long authorId, String text) {
        CallSession call = require(callId, true);
        User author = require(authorId);
        assertParticipant(call, authorId);
        return noteRepository.save(CallNote.builder()
                .callId(callId).authorId(authorId).authorName(author.getFullName()).text(text).build());
    }

    @Transactional(readOnly = true)
    public List<CallNote> notes(Long callId) {
        return noteRepository.findByCallIdOrderByCreatedAtAsc(callId);
    }

    @Transactional(readOnly = true)
    public CallSession get(Long callId) { return require(callId, true); }

    @Transactional(readOnly = true)
    public CallSession getByRoom(String roomId) {
        return callRepository.findByRoomId(roomId).orElseThrow(() -> ApiException.notFound("Room not found"));
    }

    @Transactional(readOnly = true)
    public List<CallSession> historyForPatient(Long patientId) {
        return callRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<CallSession> historyForSupport(Long supportId) {
        return callRepository.findBySupportIdOrderByCreatedAtDesc(supportId);
    }

    private CallSession require(Long callId, boolean call) {
        return callRepository.findById(callId).orElseThrow(() -> ApiException.notFound("Call not found"));
    }

    private void assertParticipant(CallSession call, Long userId) {
        boolean ok = userId.equals(call.getPatientId())
                || userId.equals(call.getSupportId())
                || userId.equals(call.getEscalatedToId());
        if (!ok) {
            // allow senior staff to join escalated calls
            User u = userRepository.findById(userId).orElse(null);
            if (u != null && u.isSenior() && call.getStatus() == CallStatus.ESCALATED) return;
            throw ApiException.forbidden("You are not a participant of this call");
        }
    }
}
