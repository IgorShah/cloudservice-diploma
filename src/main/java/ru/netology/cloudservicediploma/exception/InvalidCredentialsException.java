package ru.netology.cloudservicediploma.exception;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("Bad credentials");
    }
}
