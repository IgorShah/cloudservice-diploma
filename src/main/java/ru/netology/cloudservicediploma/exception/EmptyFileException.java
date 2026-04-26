package ru.netology.cloudservicediploma.exception;

public class EmptyFileException extends BusinessException {

    public EmptyFileException() {
        super("File must not be empty");
    }
}
