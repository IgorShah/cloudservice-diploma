package ru.netology.cloudservicediploma.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.cloudservicediploma.config.ApplicationProperties;
import ru.netology.cloudservicediploma.entity.SessionTokenEntity;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.exception.InvalidCredentialsException;
import ru.netology.cloudservicediploma.exception.UnauthorizedException;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;
import ru.netology.cloudservicediploma.service.TokenGenerator;
import ru.netology.cloudservicediploma.service.TokenHasher;

@ExtendWith(MockitoExtension.class)
class DefaultAuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionTokenRepository sessionTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private TokenHasher tokenHasher;

    private DefaultAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties(
                new ApplicationProperties.Auth(Duration.ofHours(24), 3_600_000),
                new ApplicationProperties.Storage(Path.of("storage")),
                new ApplicationProperties.Cors(List.of("http://localhost:8081")),
                List.of(new ApplicationProperties.SeedUser("user@example.com", "password"))
        );
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T10:15:30Z"), ZoneOffset.UTC);
        authenticationService = new DefaultAuthenticationService(
                userRepository,
                sessionTokenRepository,
                passwordEncoder,
                tokenGenerator,
                tokenHasher,
                clock,
                properties
        );
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        UserEntity user = new UserEntity("user@example.com", "hash");
        when(userRepository.findByLogin("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(tokenGenerator.generate()).thenReturn("token-123");
        when(tokenHasher.hash("token-123")).thenReturn("token-hash-123");

        String authToken = authenticationService.login("user@example.com", "password");

        assertThat(authToken).isEqualTo("token-123");
        verify(sessionTokenRepository).save(any(SessionTokenEntity.class));
    }

    @Test
    void loginThrowsWhenPasswordIsInvalid() {
        UserEntity user = new UserEntity("user@example.com", "hash");
        when(userRepository.findByLogin("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Bad credentials");
    }

    @Test
    void authenticateRejectsExpiredToken() {
        UserEntity user = new UserEntity("user@example.com", "hash");
        SessionTokenEntity expiredToken = new SessionTokenEntity(
                "token-123",
                user,
                Instant.parse("2026-04-25T10:15:30Z"),
                true
        );
        when(tokenHasher.hash("token-123")).thenReturn("token-hash-123");
        when(sessionTokenRepository.findByTokenHashAndActiveTrue("token-hash-123")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authenticationService.authenticate("token-123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unauthorized error");

        assertThat(expiredToken.isActive()).isFalse();
    }

    @Test
    void authenticateReturnsAuthenticatedUserForValidToken() {
        UserEntity user = new UserEntity("user@example.com", "hash");
        SessionTokenEntity sessionToken = new SessionTokenEntity(
                "token-123",
                user,
                Instant.parse("2026-04-26T10:15:30Z"),
                true
        );
        when(tokenHasher.hash("token-123")).thenReturn("token-hash-123");
        when(sessionTokenRepository.findByTokenHashAndActiveTrue("token-hash-123")).thenReturn(Optional.of(sessionToken));

        AuthenticatedUser authenticatedUser = authenticationService.authenticate("token-123");

        assertThat(authenticatedUser.login()).isEqualTo("user@example.com");
    }
}
