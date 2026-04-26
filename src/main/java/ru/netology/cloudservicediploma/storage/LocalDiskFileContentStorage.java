package ru.netology.cloudservicediploma.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import ru.netology.cloudservicediploma.config.ApplicationProperties;
import ru.netology.cloudservicediploma.exception.StorageException;
import ru.netology.cloudservicediploma.service.FileNameSanitizer;

@Component
public class LocalDiskFileContentStorage implements FileContentStorage {

    private final Path rootPath;

    public LocalDiskFileContentStorage(ApplicationProperties applicationProperties) {
        this.rootPath = applicationProperties.storage().rootPath().toAbsolutePath().normalize();
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException exception) {
            throw new StorageException("Failed to initialize file storage", exception);
        }
    }

    @Override
    public String save(Long userId, String filename, InputStream content) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        Path userDirectory = resolveUserDirectory(userId);
        Path targetPath = userDirectory.resolve(buildStorageFileName(sanitizedFilename));
        try {
            Files.createDirectories(userDirectory);
            Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return rootPath.relativize(targetPath).toString();
        } catch (IOException exception) {
            throw new StorageException("Failed to store file", exception);
        }
    }

    @Override
    public String move(Long userId, String currentStoragePath, String newFilename) {
        String sanitizedFilename = FileNameSanitizer.sanitize(newFilename);
        Path sourcePath = resolveStoredPath(currentStoragePath);
        Path targetPath = resolveUserDirectory(userId).resolve(buildStorageFileName(sanitizedFilename));
        try {
            Files.createDirectories(targetPath.getParent());
            Files.move(sourcePath, targetPath);
            return rootPath.relativize(targetPath).toString();
        } catch (IOException exception) {
            throw new StorageException("Failed to rename file", exception);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(resolveStoredPath(storagePath));
        } catch (IOException exception) {
            throw new StorageException("Failed to delete file", exception);
        }
    }

    @Override
    public Resource load(String storagePath) {
        Path filePath = resolveStoredPath(storagePath);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Failed to read file content");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new StorageException("Failed to read file content", exception);
        }
    }

    private Path resolveUserDirectory(Long userId) {
        return rootPath.resolve(String.valueOf(userId)).normalize();
    }

    private String buildStorageFileName(String sanitizedFilename) {
        return UUID.randomUUID() + "_" + sanitizedFilename;
    }

    private Path resolveStoredPath(String storagePath) {
        Path resolvedPath = rootPath.resolve(storagePath).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new StorageException("Storage path is invalid");
        }
        return resolvedPath;
    }
}
