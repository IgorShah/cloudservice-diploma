package ru.netology.cloudservicediploma.exception;

public class CloudFileNotFoundException extends BusinessException {

    public CloudFileNotFoundException() {
        super("File not found");
    }
}
