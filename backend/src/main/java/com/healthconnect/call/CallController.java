package com.healthconnect.call;

import com.healthconnect.config.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService service;
    public CallController(CallService service) { this.service = service; }

    @PostMapping("/instant")
    @PreAuthorize("hasRole('PATIENT')")
    public CallDtos.CallView startInstant(@Valid @RequestBody CallDtos.StartCallRequest req) {
        return CallDtos.CallView.of(service.startInstantCall(SecurityUtil.currentUserId(), req));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('SUPPORT')")
    public List<CallDtos.CallView> queue() {
        return service.queue().stream().map(CallDtos.CallView::of).toList();
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SUPPORT')")
    public CallDtos.CallView accept(@PathVariable Long id) {
        return CallDtos.CallView.of(service.accept(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPPORT')")
    public CallDtos.CallView reject(@PathVariable Long id) {
        return CallDtos.CallView.of(service.reject(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/cancel")
    public CallDtos.CallView cancel(@PathVariable Long id) {
        return CallDtos.CallView.of(service.cancel(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/active")
    public CallDtos.CallView active(@PathVariable Long id) {
        return CallDtos.CallView.of(service.markActive(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/end")
    public CallDtos.CallView end(@PathVariable Long id) {
        return CallDtos.CallView.of(service.end(id, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('SUPPORT')")
    public CallDtos.CallView escalate(@PathVariable Long id, @Valid @RequestBody CallDtos.EscalateRequest req) {
        return CallDtos.CallView.of(service.escalate(id, SecurityUtil.currentUserId(), req.reason()));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasRole('PATIENT')")
    public CallDtos.CallView rate(@PathVariable Long id, @RequestBody CallDtos.RateRequest req) {
        return CallDtos.CallView.of(service.rate(id, SecurityUtil.currentUserId(), req.rating(), req.comment()));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasRole('SUPPORT')")
    public CallDtos.NoteView addNote(@PathVariable Long id, @Valid @RequestBody CallDtos.NoteRequest req) {
        return CallDtos.NoteView.of(service.addNote(id, SecurityUtil.currentUserId(), req.text()));
    }

    @GetMapping("/{id}")
    public CallDtos.CallDetail detail(@PathVariable Long id) {
        var notes = service.notes(id).stream().map(CallDtos.NoteView::of).toList();
        return new CallDtos.CallDetail(CallDtos.CallView.of(service.get(id)), notes);
    }

    @GetMapping("/room/{roomId}")
    public CallDtos.CallView byRoom(@PathVariable String roomId) {
        return CallDtos.CallView.of(service.getByRoom(roomId));
    }

    @GetMapping("/history")
    public List<CallDtos.CallView> history() {
        Long uid = SecurityUtil.currentUserId();
        var asPatient = service.historyForPatient(uid);
        var asSupport = service.historyForSupport(uid);
        return java.util.stream.Stream.concat(asPatient.stream(), asSupport.stream())
                .distinct().map(CallDtos.CallView::of).toList();
    }
}
