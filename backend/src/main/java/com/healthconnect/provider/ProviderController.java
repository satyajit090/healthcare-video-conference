package com.healthconnect.provider;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService service;
    public ProviderController(ProviderService service) { this.service = service; }

    // public: clients learn which provider to use for a call
    @GetMapping("/active")
    public List<ProviderDtos.View> active() {
        return service.active().stream().map(ProviderDtos.View::of).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProviderDtos.View> all() {
        return service.all().stream().map(ProviderDtos.View::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderDtos.View create(@Valid @RequestBody ProviderDtos.UpsertRequest r) {
        return ProviderDtos.View.of(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderDtos.View update(@PathVariable Long id, @Valid @RequestBody ProviderDtos.UpsertRequest r) {
        return ProviderDtos.View.of(service.update(id, r));
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderDtos.View setDefault(@PathVariable Long id) {
        return ProviderDtos.View.of(service.setDefault(id));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderDtos.View toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return ProviderDtos.View.of(service.toggle(id, Boolean.TRUE.equals(body.get("enabled"))));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderDtos.View test(@PathVariable Long id) {
        return ProviderDtos.View.of(service.test(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
