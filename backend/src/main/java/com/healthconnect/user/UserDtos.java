package com.healthconnect.user;

import com.healthconnect.common.Role;

public class UserDtos {
    public record UserView(Long id, String email, String fullName, Role role,
                           String availability, String specialty, String language, boolean senior) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                    u.getAvailability(), u.getSpecialty(), u.getLanguage(), u.isSenior());
        }
    }
    public record AvailabilityRequest(String availability) {}
    public record ProfileUpdate(String fullName, String specialty, String language) {}
}
