package com.healthconnect.provider;

import jakarta.validation.constraints.NotBlank;

public class ProviderDtos {
    public record UpsertRequest(
            @NotBlank String name, @NotBlank String type, Boolean enabled, Boolean isDefault,
            Integer priority, String apiKey, String apiSecret,
            Integer maxParticipants, Integer maxDurationMinutes) {}

    // apiKey/secret masked; never expose secrets to the client.
    public record View(Long id, String name, String type, boolean enabled, boolean isDefault,
                       int priority, boolean hasCredentials, Integer maxParticipants,
                       Integer maxDurationMinutes, String lastTestStatus) {
        public static View of(VideoProvider p) {
            boolean creds = p.getApiKey() != null && !p.getApiKey().isBlank();
            return new View(p.getId(), p.getName(), p.getType(), p.isEnabled(), p.isDefault(),
                    p.getPriority(), creds, p.getMaxParticipants(), p.getMaxDurationMinutes(),
                    p.getLastTestStatus());
        }
    }
}
