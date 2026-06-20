package com.healthconnect.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Google's Gemini generateContent REST API.
 * If no API key is configured the service degrades gracefully and returns a
 * helpful placeholder, so the rest of the app keeps working without AI.
 */
@Service
public class GeminiService {

    private final WebClient client;
    private final String apiKey;
    private final String model;

    public GeminiService(WebClient geminiWebClient,
                         @Value("${app.gemini.api-key}") String apiKey,
                         @Value("${app.gemini.model}") String model) {
        this.client = geminiWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean enabled() { return apiKey != null && !apiKey.isBlank(); }

    public String generate(String systemInstruction, String userPrompt) {
        if (!enabled()) {
            return "[AI assistant is not configured. Set GEMINI_API_KEY to enable triage, "
                    + "summaries and live suggestions.]";
        }
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.4, "maxOutputTokens", 800)
        );
        try {
            JsonNode resp = client.post()
                    .uri(uri -> uri.path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey).build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            if (resp == null) return "[No response from AI service]";
            JsonNode parts = resp.path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            parts.forEach(p -> sb.append(p.path("text").asText("")));
            String text = sb.toString().trim();
            return text.isBlank() ? "[AI returned an empty response]" : text;
        } catch (Exception e) {
            return "[AI service error: " + e.getMessage() + "]";
        }
    }
}
