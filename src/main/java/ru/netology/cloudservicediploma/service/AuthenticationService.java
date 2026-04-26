package ru.netology.cloudservicediploma.service;

import ru.netology.cloudservicediploma.security.AuthenticatedUser;

public interface AuthenticationService {

    String login(String login, String password);

    void logout(String authToken);

    AuthenticatedUser authenticate(String authToken);
}
