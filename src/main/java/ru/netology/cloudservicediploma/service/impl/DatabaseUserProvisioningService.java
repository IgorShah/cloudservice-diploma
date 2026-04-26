package ru.netology.cloudservicediploma.service.impl;

import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import ru.netology.cloudservicediploma.config.ApplicationProperties;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.service.UserProvisioningService;

@Service
public class DatabaseUserProvisioningService implements UserProvisioningService {

    private final ApplicationProperties applicationProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseUserProvisioningService(
            ApplicationProperties applicationProperties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.applicationProperties = applicationProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void ensureUsersExist() {
        for (ApplicationProperties.SeedUser seedUser : applicationProperties.users()) {
            userRepository.findByLogin(seedUser.login())
                    .orElseGet(() -> userRepository.save(
                            new UserEntity(seedUser.login(), passwordEncoder.encode(seedUser.password()))
                    ));
        }
    }
}
