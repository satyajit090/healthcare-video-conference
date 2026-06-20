package com.healthconnect.notification;

import com.healthconnect.config.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;
    public NotificationController(NotificationRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Notification> mine() {
        return repository.findTop50ByRecipientUserIdOrderByCreatedAtDesc(SecurityUtil.currentUserId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("count", repository.countByRecipientUserIdAndReadFalse(SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        repository.findById(id).ifPresent(n -> {
            if (n.getRecipientUserId().equals(SecurityUtil.currentUserId())) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }
}
