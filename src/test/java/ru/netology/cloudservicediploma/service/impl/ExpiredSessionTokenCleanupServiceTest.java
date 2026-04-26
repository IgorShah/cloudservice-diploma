package ru.netology.cloudservicediploma.service.impl;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;

@ExtendWith(MockitoExtension.class)
class ExpiredSessionTokenCleanupServiceTest {

    @Mock
    private SessionTokenRepository sessionTokenRepository;

    private ExpiredSessionTokenCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T10:15:30Z"), ZoneOffset.UTC);
        cleanupService = new ExpiredSessionTokenCleanupService(sessionTokenRepository, clock);
    }

    @Test
    void deleteExpiredTokensRemovesTokensExpiredBeforeNow() {
        cleanupService.deleteExpiredTokens();

        verify(sessionTokenRepository).deleteExpiredBefore(Instant.parse("2026-04-25T10:15:30Z"));
    }
}
