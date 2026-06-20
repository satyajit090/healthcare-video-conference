package com.healthconnect.config;

import com.healthconnect.common.Role;
import com.healthconnect.provider.VideoProvider;
import com.healthconnect.provider.VideoProviderRepository;
import com.healthconnect.user.User;
import com.healthconnect.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final VideoProviderRepository providers;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, VideoProviderRepository providers, PasswordEncoder encoder) {
        this.users = users;
        this.providers = providers;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            users.save(User.builder().email("admin@health.dev").passwordHash(encoder.encode("admin123"))
                    .fullName("System Admin").role(Role.ADMIN).availability("OFFLINE").language("en").build());

            users.save(User.builder().email("dr.smith@health.dev").passwordHash(encoder.encode("support123"))
                    .fullName("Dr. Anita Smith").role(Role.SUPPORT).availability("AVAILABLE")
                    .specialty("General Medicine").language("en").senior(true).build());

            users.save(User.builder().email("yoga.ravi@health.dev").passwordHash(encoder.encode("support123"))
                    .fullName("Ravi (Yoga Instructor)").role(Role.SUPPORT).availability("AVAILABLE")
                    .specialty("Yoga").language("en").build());

            users.save(User.builder().email("patient@health.dev").passwordHash(encoder.encode("patient123"))
                    .fullName("John Patient").role(Role.PATIENT).availability("OFFLINE").language("en").build());

            System.out.println(">>> Seeded demo users (admin@/dr.smith@/yoga.ravi@/patient@health.dev)");
        }

        if (providers.count() == 0) {
            providers.save(VideoProvider.builder().name("Internal WebRTC").type("INTERNAL")
                    .enabled(true).isDefault(true).priority(1).maxParticipants(8)
                    .maxDurationMinutes(120).lastTestStatus("OK").build());
            providers.save(VideoProvider.builder().name("Zoom").type("ZOOM")
                    .enabled(false).priority(10).maxParticipants(100)
                    .maxDurationMinutes(40).lastTestStatus("UNTESTED").build());
            providers.save(VideoProvider.builder().name("Microsoft Teams").type("TEAMS")
                    .enabled(false).priority(20).maxParticipants(250)
                    .maxDurationMinutes(60).lastTestStatus("UNTESTED").build());
            System.out.println(">>> Seeded video providers (Internal WebRTC default)");
        }
    }
}
