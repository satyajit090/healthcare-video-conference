package com.healthconnect.provider;

import com.healthconnect.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProviderService {

    private final VideoProviderRepository repository;
    public ProviderService(VideoProviderRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<VideoProvider> all() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public List<VideoProvider> active() { return repository.findByEnabledTrueOrderByPriorityAsc(); }

    @Transactional
    public VideoProvider create(ProviderDtos.UpsertRequest r) {
        if (repository.existsByName(r.name())) throw ApiException.badRequest("Provider name already exists");
        VideoProvider p = VideoProvider.builder()
                .name(r.name()).type(r.type())
                .enabled(r.enabled() == null || r.enabled())
                .priority(r.priority() == null ? 100 : r.priority())
                .apiKey(r.apiKey()).apiSecret(r.apiSecret())
                .maxParticipants(r.maxParticipants()).maxDurationMinutes(r.maxDurationMinutes())
                .lastTestStatus("UNTESTED")
                .build();
        if (Boolean.TRUE.equals(r.isDefault())) clearDefault();
        p.setDefault(Boolean.TRUE.equals(r.isDefault()));
        return repository.save(p);
    }

    @Transactional
    public VideoProvider update(Long id, ProviderDtos.UpsertRequest r) {
        VideoProvider p = get(id);
        p.setName(r.name());
        p.setType(r.type());
        if (r.enabled() != null) p.setEnabled(r.enabled());
        if (r.priority() != null) p.setPriority(r.priority());
        if (r.apiKey() != null) p.setApiKey(r.apiKey());
        if (r.apiSecret() != null) p.setApiSecret(r.apiSecret());
        p.setMaxParticipants(r.maxParticipants());
        p.setMaxDurationMinutes(r.maxDurationMinutes());
        if (Boolean.TRUE.equals(r.isDefault())) { clearDefault(); p.setDefault(true); }
        return repository.save(p);
    }

    @Transactional
    public VideoProvider setDefault(Long id) {
        VideoProvider p = get(id);
        clearDefault();
        p.setDefault(true);
        p.setEnabled(true);
        return repository.save(p);
    }

    @Transactional
    public VideoProvider toggle(Long id, boolean enabled) {
        VideoProvider p = get(id);
        p.setEnabled(enabled);
        return repository.save(p);
    }

    /** Simulated connection test. INTERNAL always OK; external requires credentials. */
    @Transactional
    public VideoProvider test(Long id) {
        VideoProvider p = get(id);
        boolean ok = "INTERNAL".equalsIgnoreCase(p.getType())
                || (p.getApiKey() != null && !p.getApiKey().isBlank());
        p.setLastTestStatus(ok ? "OK" : "FAILED");
        p.setLastTestedAt(Instant.now());
        return repository.save(p);
    }

    @Transactional
    public void delete(Long id) { repository.delete(get(id)); }

    public VideoProvider get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Provider not found"));
    }

    private void clearDefault() {
        repository.findByIsDefaultTrue().ifPresent(d -> { d.setDefault(false); repository.save(d); });
    }
}
