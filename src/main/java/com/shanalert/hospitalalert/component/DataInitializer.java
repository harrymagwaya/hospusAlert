package com.shanalert.hospitalalert.component;

import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("system_admin")
                    .email("mwijukaharoid@gmail.com")
                    .password(passwordEncoder.encode("YourSecurePassword123!"))
                    .role(UserRole.ADMIN)
                    .userStatus(UserStatus.ACTIVE)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> Initial Super Admin Created!");
        }
    }
}