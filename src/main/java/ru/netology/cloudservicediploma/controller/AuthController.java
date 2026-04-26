package ru.netology.cloudservicediploma.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.cloudservicediploma.dto.request.LoginRequest;
import ru.netology.cloudservicediploma.dto.response.AuthTokenResponse;
import ru.netology.cloudservicediploma.security.AuthTokenHeader;
import ru.netology.cloudservicediploma.service.AuthenticationService;

@RestController
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        String authToken = authenticationService.login(request.login(), request.password());
        return new AuthTokenResponse(authToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(AuthTokenHeader.NAME) String authTokenHeader) {
        authenticationService.logout(AuthTokenHeader.normalize(authTokenHeader));
        return ResponseEntity.ok().build();
    }
}
