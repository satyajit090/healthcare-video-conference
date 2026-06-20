package com.healthconnect.call;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CallNoteRepository extends JpaRepository<CallNote, Long> {
    List<CallNote> findByCallIdOrderByCreatedAtAsc(Long callId);
}
