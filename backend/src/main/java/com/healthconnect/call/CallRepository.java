package com.healthconnect.call;

import com.healthconnect.common.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CallRepository extends JpaRepository<CallSession, Long> {
    Optional<CallSession> findByRoomId(String roomId);

    List<CallSession> findByStatusOrderByUrgentDescCreatedAtAsc(CallStatus status);

    List<CallSession> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<CallSession> findBySupportIdOrderByCreatedAtDesc(Long supportId);

    long countByStatus(CallStatus status);

    @Query("select avg(c.rating) from CallSession c where c.rating is not null")
    Double averageRating();

    @Query("select avg(c.durationSeconds) from CallSession c where c.durationSeconds is not null")
    Double averageDuration();

    List<CallSession> findByCreatedAtBetween(Instant from, Instant to);
}
