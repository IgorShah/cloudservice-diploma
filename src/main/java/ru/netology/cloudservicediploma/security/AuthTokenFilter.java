package ru.netology.cloudservicediploma.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
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
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    Collections.emptyList()
            ));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
