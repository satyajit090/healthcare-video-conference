package com.healthconnect.user;

import com.healthconnect.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndAvailability(Role role, String availability);
    List<User> findByRoleAndSeniorTrue(Role role);
}
