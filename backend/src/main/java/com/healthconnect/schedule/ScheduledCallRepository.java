package com.healthconnect.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ScheduledCallRepository extends JpaRepository<ScheduledCall, Long> {
    List<ScheduledCall> findByPatientIdOrderByScheduledAtAsc(Long patientId);
    List<ScheduledCall> findBySupportIdOrderByScheduledAtAsc(Long supportId);
    List<ScheduledCall> findByStatusAndScheduledAtBetween(String status, Instant from, Instant to);
}
