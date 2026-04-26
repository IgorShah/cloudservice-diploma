package ru.netology.cloudservicediploma.service.impl;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;

@Service
public class ExpiredSessionTokenCleanupService {

    private final SessionTokenRepository sessionTokenRepository;
    private final Clock clock;

    @Autowired
    public ExpiredSessionTokenCleanupService(SessionTokenRepository sessionTokenRepository) {
        this(sessionTokenRepository, Clock.systemUTC());
    }

    ExpiredSessionTokenCleanupService(SessionTokenRepository sessionTokenRepository, Clock clock) {
        this.sessionTokenRepository = sessionTokenRepository;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.auth.expired-token-cleanup-delay-ms:3600000}")
    public void deleteExpiredTokens() {
        sessionTokenRepository.deleteExpiredBefore(Instant.now(clock));
    }
}
