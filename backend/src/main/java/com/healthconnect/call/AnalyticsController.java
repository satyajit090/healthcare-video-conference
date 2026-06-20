package com.healthconnect.call;

import com.healthconnect.common.CallStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final CallRepository calls;
    public AnalyticsController(CallRepository calls) { this.calls = calls; }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCalls", calls.count());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (CallStatus s : CallStatus.values()) byStatus.put(s.name(), calls.countByStatus(s));
        out.put("byStatus", byStatus);
        Double avgRating = calls.averageRating();
        Double avgDuration = calls.averageDuration();
        out.put("averageRating", avgRating == null ? 0 : Math.round(avgRating * 100.0) / 100.0);
        out.put("averageDurationSeconds", avgDuration == null ? 0 : Math.round(avgDuration));
        out.put("completedCalls", calls.countByStatus(CallStatus.COMPLETED));
        return out;
    }
}
