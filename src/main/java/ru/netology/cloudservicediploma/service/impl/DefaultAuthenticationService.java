package ru.netology.cloudservicediploma.service.impl;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudservicediploma.config.ApplicationProperties;
import ru.netology.cloudservicediploma.dto.response.AuthTokenResponse;
import ru.netology.cloudservicediploma.entity.SessionTokenEntity;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.exception.BadRequestException;
import ru.netology.cloudservicediploma.exception.UnauthorizedException;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;
import ru.netology.cloudservicediploma.service.AuthenticationService;
import ru.netology.cloudservicediploma.service.TokenGenerator;

@Service
public class DefaultAuthenticationService implements AuthenticationService {

    private final UserRepository userRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final Clock clock;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public DefaultAuthenticationService(
            UserRepository userRepository,
            SessionTokenRepository sessionTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator,
            ApplicationProperties applicationProperties
    ) {
        this(userRepository, sessionTokenRepository, passwordEncoder, tokenGenerator, Clock.systemUTC(), applicationProperties);
    }

    DefaultAuthenticationService(
            UserRepository userRepository,
            SessionTokenRepository sessionTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator,
            Clock clock,
            ApplicationProperties applicationProperties
    ) {
        this.userRepository = userRepository;
        this.sessionTokenRepository = sessionTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.applicationProperties = applicationProperties;
    }

    @Override
    @Transactional
    public AuthTokenResponse login(String login, String password) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new BadRequestException("Bad credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Bad credentials");
        }

        Instant now = Instant.now(clock);
        SessionTokenEntity sessionToken = new SessionTokenEntity(
                tokenGenerator.generate(),
                user,
                now.plus(applicationProperties.auth().sessionTtl()),
                true
        );
        sessionTokenRepository.save(sessionToken);
        return new AuthTokenResponse(sessionToken.getToken());
    }

    @Override
    @Transactional
    public void logout(String authToken) {
        SessionTokenEntity sessionToken = sessionTokenRepository.findByTokenAndActiveTrue(authToken)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized error"));
        sessionToken.deactivate();
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String authToken) {
        SessionTokenEntity sessionToken = sessionTokenRepository.findByTokenAndActiveTrue(authToken)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized error"));

        Instant now = Instant.now(clock);
        if (sessionToken.isExpired(now)) {
            sessionToken.deactivate();
            throw new UnauthorizedException("Unauthorized error");
        }

        UserEntity user = sessionToken.getUser();
        return new AuthenticatedUser(user.getId(), user.getLogin());
    }
}
