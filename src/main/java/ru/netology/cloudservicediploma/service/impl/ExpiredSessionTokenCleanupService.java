package ru.netology.cloudservicediploma.service.impl;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;

@Service
public class ExpiredSessionTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSessionTokenCleanupService.class);

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
        int deletedCount = sessionTokenRepository.deleteExpiredBefore(Instant.now(clock));
        if (deletedCount > 0) {
            log.info("Expired session tokens deleted: count={}", deletedCount);
        }
    }
}
