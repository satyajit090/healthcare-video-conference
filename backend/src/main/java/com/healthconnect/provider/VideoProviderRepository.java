package com.healthconnect.provider;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VideoProviderRepository extends JpaRepository<VideoProvider, Long> {
    List<VideoProvider> findByEnabledTrueOrderByPriorityAsc();
    Optional<VideoProvider> findByIsDefaultTrue();
    boolean existsByName(String name);
}
