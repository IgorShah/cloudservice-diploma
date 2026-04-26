package ru.netology.cloudservicediploma.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.netology.cloudservicediploma.exception.UnauthorizedException;
import ru.netology.cloudservicediploma.service.AuthenticationService;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final AuthenticationService authenticationService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public AuthTokenFilter(
            AuthenticationService authenticationService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.authenticationService = authenticationService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/login".equals(requestUri)
                || "/actuator/health".equals(requestUri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authToken = AuthTokenHeader.normalize(request.getHeader(AuthTokenHeader.NAME));
            if (authToken == null || authToken.isBlank()) {
                throw new UnauthorizedException("Unauthorized error");
            }

            AuthenticatedUser authenticatedUser = authenticationService.authenticate(authToken);
            log.debug("Auth token accepted: userId={}", authenticatedUser.id());
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    Collections.emptyList()
            ));
            SecurityContextHolder.setContext(securityContext);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            log.debug("Auth token rejected: method={}, uri={}", request.getMethod(), request.getRequestURI());
            handlerExceptionResolver.resolveException(request, response, null, exception);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
