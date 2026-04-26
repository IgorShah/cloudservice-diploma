package ru.netology.cloudservicediploma.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends CloudServiceException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
