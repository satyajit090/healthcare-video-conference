package com.healthconnect.user;

import com.healthconnect.common.ApiException;
import com.healthconnect.common.Role;
import com.healthconnect.config.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Set<String> VALID = Set.of("AVAILABLE", "BUSY", "OFFLINE");
    private final UserRepository userRepository;
    public UserController(UserRepository userRepository) { this.userRepository = userRepository; }

    private User current() {
        return userRepository.findById(SecurityUtil.currentUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @GetMapping("/me")
    public UserDtos.UserView me() { return UserDtos.UserView.of(current()); }

    @PutMapping("/me")
    public UserDtos.UserView updateProfile(@RequestBody UserDtos.ProfileUpdate req) {
        User u = current();
        if (req.fullName() != null && !req.fullName().isBlank()) u.setFullName(req.fullName().trim());
        if (req.specialty() != null) u.setSpecialty(req.specialty());
        if (req.language() != null && !req.language().isBlank()) u.setLanguage(req.language());
        return UserDtos.UserView.of(userRepository.save(u));
    }

    @PatchMapping("/me/availability")
    public UserDtos.UserView setAvailability(@RequestBody UserDtos.AvailabilityRequest req) {
        User u = current();
        if (u.getRole() != Role.SUPPORT) throw ApiException.forbidden("Only support staff have availability");
        String a = req.availability() == null ? "" : req.availability().toUpperCase();
        if (!VALID.contains(a)) throw ApiException.badRequest("availability must be AVAILABLE, BUSY or OFFLINE");
        u.setAvailability(a);
        return UserDtos.UserView.of(userRepository.save(u));
    }

    // Patients see who is online before initiating a call (acceptance criterion).
    @GetMapping("/support")
    public List<UserDtos.UserView> supportStaff(@RequestParam(required = false) String language) {
        return userRepository.findByRole(Role.SUPPORT).stream()
                .filter(u -> language == null || language.isBlank() || language.equalsIgnoreCase(u.getLanguage()))
                .map(UserDtos.UserView::of)
                .toList();
    }
}
