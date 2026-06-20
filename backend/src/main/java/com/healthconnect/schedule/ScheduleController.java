package com.healthconnect.schedule;

import com.healthconnect.config.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService service;
    public ScheduleController(ScheduleService service) { this.service = service; }

    @PostMapping
    public ScheduleDtos.View book(@Valid @RequestBody ScheduleDtos.BookRequest req) {
        return ScheduleDtos.View.of(service.book(SecurityUtil.currentUserId(), req));
    }

    @GetMapping
    public List<ScheduleDtos.View> mine() {
        return service.forUser(SecurityUtil.currentUserId()).stream().map(ScheduleDtos.View::of).toList();
    }

    @PostMapping("/{id}/cancel")
    public ScheduleDtos.View cancel(@PathVariable Long id) {
        return ScheduleDtos.View.of(service.cancel(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/reschedule")
    public ScheduleDtos.View reschedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ScheduleDtos.View.of(service.reschedule(id, SecurityUtil.currentUserId(),
                Instant.parse(body.get("scheduledAt"))));
    }
}
