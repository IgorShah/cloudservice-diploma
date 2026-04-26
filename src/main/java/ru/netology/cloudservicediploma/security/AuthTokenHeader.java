package ru.netology.cloudservicediploma.security;

public final class AuthTokenHeader {

    public static final String NAME = "auth-token";

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthTokenHeader() {
    }

    public static String normalize(String authTokenHeader) {
        if (authTokenHeader == null) {
            return null;
        }
        if (authTokenHeader.startsWith(BEARER_PREFIX)) {
            return authTokenHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return authTokenHeader.trim();
    }
}
