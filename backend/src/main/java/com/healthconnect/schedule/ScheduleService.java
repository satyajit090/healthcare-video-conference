package com.healthconnect.schedule;

import com.healthconnect.common.ApiException;
import com.healthconnect.common.Role;
import com.healthconnect.notification.AppEvent;
import com.healthconnect.notification.EventPublisher;
import com.healthconnect.user.User;
import com.healthconnect.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduleService {

    private final ScheduledCallRepository repository;
    private final UserRepository userRepository;
    private final EventPublisher events;

    public ScheduleService(ScheduledCallRepository repository, UserRepository userRepository, EventPublisher events) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.events = events;
    }

    @Transactional
    public ScheduledCall book(Long patientId, ScheduleDtos.BookRequest req) {
        User patient = userRepository.findById(patientId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (req.scheduledAt().isBefore(Instant.now())) throw ApiException.badRequest("Cannot schedule in the past");

        ScheduledCall.ScheduledCallBuilder b = ScheduledCall.builder()
                .patientId(patient.getId())
                .patientName(patient.getFullName())
                .caseType(req.caseType())
                .reason(req.reason())
                .scheduledAt(req.scheduledAt())
                .durationMinutes(req.durationMinutes() == null ? 30 : req.durationMinutes())
                .timezone(req.timezone())
                .roomId(UUID.randomUUID().toString())
                .status("SCHEDULED");

        if (req.supportId() != null) {
            User support = userRepository.findById(req.supportId())
                    .orElseThrow(() -> ApiException.notFound("Support person not found"));
            if (support.getRole() != Role.SUPPORT) throw ApiException.badRequest("Selected user is not support staff");
            b.supportId(support.getId()).supportName(support.getFullName());
        }
        ScheduledCall saved = repository.save(b.build());

        if (saved.getSupportId() != null) {
            events.publish(AppEvent.of("CALL_SCHEDULED", saved.getSupportId(),
                    "New scheduled call",
                    patient.getFullName() + " booked a call for " + saved.getScheduledAt(),
                    Map.of("scheduledId", saved.getId(), "roomId", saved.getRoomId())));
        }
        events.publish(AppEvent.of("CALL_SCHEDULED", patient.getId(),
                "Call scheduled", "Your call is booked for " + saved.getScheduledAt(),
                Map.of("scheduledId", saved.getId(), "roomId", saved.getRoomId())));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScheduledCall> forUser(Long userId) {
        var asPatient = repository.findByPatientIdOrderByScheduledAtAsc(userId);
        if (!asPatient.isEmpty()) return asPatient;
        return repository.findBySupportIdOrderByScheduledAtAsc(userId);
    }

    @Transactional
    public ScheduledCall cancel(Long id, Long userId) {
        ScheduledCall s = repository.findById(id).orElseThrow(() -> ApiException.notFound("Not found"));
        if (!s.getPatientId().equals(userId) && !userId.equals(s.getSupportId()))
            throw ApiException.forbidden("Not your scheduled call");
        s.setStatus("CANCELLED");
        return repository.save(s);
    }

    @Transactional
    public ScheduledCall reschedule(Long id, Long userId, Instant newTime) {
        ScheduledCall s = repository.findById(id).orElseThrow(() -> ApiException.notFound("Not found"));
        if (!s.getPatientId().equals(userId)) throw ApiException.forbidden("Only the patient can reschedule");
        if (newTime.isBefore(Instant.now())) throw ApiException.badRequest("Cannot reschedule into the past");
        s.setScheduledAt(newTime);
        s.setReminded24h(false);
        s.setReminded1h(false);
        return repository.save(s);
    }

    /** Reminder sweep: runs every minute, fires 24h and 1h reminders. */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void reminderSweep() {
        Instant now = Instant.now();
        for (ScheduledCall s : repository.findByStatusAndScheduledAtBetween(
                "SCHEDULED", now, now.plus(Duration.ofHours(25)))) {
            long minutesAway = Duration.between(now, s.getScheduledAt()).toMinutes();
            if (!s.isReminded24h() && minutesAway <= 24 * 60 && minutesAway > 60) {
                fireReminder(s, "in about 24 hours");
                s.setReminded24h(true);
                repository.save(s);
            } else if (!s.isReminded1h() && minutesAway <= 60) {
                fireReminder(s, "in about 1 hour");
                s.setReminded1h(true);
                repository.save(s);
            }
        }
    }

    private void fireReminder(ScheduledCall s, String when) {
        events.publish(AppEvent.of("CALL_REMINDER", s.getPatientId(),
                "Upcoming call", "Reminder: your support call is " + when,
                Map.of("scheduledId", s.getId(), "roomId", s.getRoomId())));
        if (s.getSupportId() != null) {
            events.publish(AppEvent.of("CALL_REMINDER", s.getSupportId(),
                    "Upcoming call", "Reminder: a scheduled call is " + when,
                    Map.of("scheduledId", s.getId(), "roomId", s.getRoomId())));
        }
    }
}
