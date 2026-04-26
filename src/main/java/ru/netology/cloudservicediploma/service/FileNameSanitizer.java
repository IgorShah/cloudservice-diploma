package ru.netology.cloudservicediploma.service;

import ru.netology.cloudservicediploma.exception.InvalidFileNameException;

public final class FileNameSanitizer {

    private FileNameSanitizer() {
    }

    public static String sanitize(String rawFileName) {
        if (rawFileName == null) {
            throw new InvalidFileNameException("Filename must not be empty");
        }

        String fileName = rawFileName.trim();
        if (fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || ".".equals(fileName)
                || "..".equals(fileName)) {
            throw new InvalidFileNameException("Filename is invalid");
        }

        return fileName;
    }
}
