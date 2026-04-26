package ru.netology.cloudservicediploma.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends CloudServiceException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
