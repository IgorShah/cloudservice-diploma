package ru.netology.cloudservicediploma.exception;

import org.springframework.http.HttpStatus;

public class CloudServiceException extends RuntimeException {

    private final HttpStatus status;

    public CloudServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
