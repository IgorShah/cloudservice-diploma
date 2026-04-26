package ru.netology.cloudservicediploma.exception;

public class FileAlreadyExistsException extends BusinessException {

    public FileAlreadyExistsException() {
        super("File already exists");
    }
}
