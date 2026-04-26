package ru.netology.cloudservicediploma.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super("User not found");
    }
}
