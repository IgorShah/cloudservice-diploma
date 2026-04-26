package ru.netology.cloudservicediploma.service;

import org.springframework.core.io.Resource;

public record DownloadedFile(
        String filename,
        long size,
        String contentType,
        Resource resource
) {
}
