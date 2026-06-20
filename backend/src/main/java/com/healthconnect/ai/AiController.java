package com.healthconnect.ai;

import com.healthconnect.call.CallService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService gemini;
    private final CallService callService;

    public AiController(GeminiService gemini, CallService callService) {
        this.gemini = gemini;
        this.callService = callService;
    }

    public record AssistRequest(@NotBlank String prompt, String context) {}
    public record TranslateRequest(@NotBlank String text, @NotBlank String targetLanguage) {}

    @GetMapping("/status")
    public Map<String, Boolean> status() { return Map.of("enabled", gemini.enabled()); }

    /** Live in-call assistant for support staff: phrasing help, follow-up questions, resources. */
    @PostMapping("/assist")
    public Map<String, String> assist(@RequestBody AssistRequest req) {
        String out = gemini.generate(
                "You are a supportive assistant for a healthcare and yoga support agent during a live video call. "
              + "Give brief, practical, non-diagnostic guidance. Keep it under 120 words.",
                (req.context() != null ? "Call context: " + req.context() + "\n\n" : "") + "Agent asks: " + req.prompt());
        return Map.of("response", out);
    }

    /** Summarize a finished call from its reason + support notes. */
    @PostMapping("/summary/{callId}")
    public Map<String, String> summary(@PathVariable Long callId) {
        var call = callService.get(callId);
        StringBuilder ctx = new StringBuilder();
        ctx.append("Reason: ").append(call.getReason()).append("\n");
        callService.notes(callId).forEach(n -> ctx.append("Note: ").append(n.getText()).append("\n"));
        String out = gemini.generate(
                "Summarize this support session into a short, structured note: Summary, Key points, Follow-up actions. "
              + "No diagnosis. Max 150 words.", ctx.toString());
        return Map.of("summary", out);
    }

    /** Real-time translation helper (multi-language support user story). */
    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody TranslateRequest req) {
        String out = gemini.generate(
                "You are a translation engine. Translate the user's text into the target language. "
              + "Return ONLY the translation, no explanations.",
                "Target language: " + req.targetLanguage() + "\nText: " + req.text());
        return Map.of("translation", out);
    }
}
