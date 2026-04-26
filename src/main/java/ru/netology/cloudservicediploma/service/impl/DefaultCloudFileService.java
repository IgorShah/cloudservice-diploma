package ru.netology.cloudservicediploma.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservicediploma.dto.response.FileResponse;
import ru.netology.cloudservicediploma.entity.StoredFileEntity;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.exception.BadRequestException;
import ru.netology.cloudservicediploma.exception.StorageException;
import ru.netology.cloudservicediploma.repository.StoredFileRepository;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;
import ru.netology.cloudservicediploma.service.CloudFileService;
import ru.netology.cloudservicediploma.service.DownloadedFile;
import ru.netology.cloudservicediploma.storage.FileContentStorage;
import ru.netology.cloudservicediploma.service.FileNameSanitizer;

@Service
public class DefaultCloudFileService implements CloudFileService {

    private final StoredFileRepository storedFileRepository;
    private final UserRepository userRepository;
    private final FileContentStorage fileContentStorage;
    private final Clock clock;

    @Autowired
    public DefaultCloudFileService(
            StoredFileRepository storedFileRepository,
            UserRepository userRepository,
            FileContentStorage fileContentStorage
    ) {
        this(storedFileRepository, userRepository, fileContentStorage, Clock.systemUTC());
    }

    DefaultCloudFileService(
            StoredFileRepository storedFileRepository,
            UserRepository userRepository,
            FileContentStorage fileContentStorage,
            Clock clock
    ) {
        this.storedFileRepository = storedFileRepository;
        this.userRepository = userRepository;
        this.fileContentStorage = fileContentStorage;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> listFiles(AuthenticatedUser user, int limit) {
        return storedFileRepository.findAllByUserIdOrderByUploadedAtDesc(user.id(), PageRequest.of(0, limit))
                .stream()
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .toList();
    }

    @Override
    @Transactional
    public void uploadFile(AuthenticatedUser user, String filename, MultipartFile file) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (storedFileRepository.existsByUserIdAndFilename(user.id(), sanitizedFilename)) {
            throw new BadRequestException("File already exists");
        }

        UserEntity persistedUser = loadUser(user.id());
        try (InputStream content = file.getInputStream()) {
            String storagePath = fileContentStorage.save(user.id(), sanitizedFilename, content);
            Instant now = Instant.now(clock);
            StoredFileEntity storedFile = new StoredFileEntity(
                    persistedUser,
                    sanitizedFilename,
                    file.getSize(),
                    file.getContentType(),
                    storagePath,
                    now,
                    now
            );
            storedFileRepository.save(storedFile);
        } catch (IOException exception) {
            throw new StorageException("Failed to read uploaded file", exception);
        }
    }

    @Override
    @Transactional
    public void deleteFile(AuthenticatedUser user, String filename) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(user.id(), sanitizedFilename)
                .orElseThrow(() -> new BadRequestException("File not found"));
        fileContentStorage.delete(storedFile.getStoragePath());
        storedFileRepository.delete(storedFile);
    }

    @Override
    @Transactional
    public void renameFile(AuthenticatedUser user, String sourceFilename, String targetFilename) {
        String sanitizedSourceFilename = FileNameSanitizer.sanitize(sourceFilename);
        String sanitizedTargetFilename = FileNameSanitizer.sanitize(targetFilename);
        if (sanitizedSourceFilename.equals(sanitizedTargetFilename)) {
            return;
        }
        if (storedFileRepository.existsByUserIdAndFilename(user.id(), sanitizedTargetFilename)) {
            throw new BadRequestException("File already exists");
        }

        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(user.id(), sanitizedSourceFilename)
                .orElseThrow(() -> new BadRequestException("File not found"));
        String newStoragePath = fileContentStorage.move(user.id(), storedFile.getStoragePath(), sanitizedTargetFilename);
        storedFile.rename(sanitizedTargetFilename, newStoragePath, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile downloadFile(AuthenticatedUser user, String filename) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(user.id(), sanitizedFilename)
                .orElseThrow(() -> new BadRequestException("File not found"));

        return new DownloadedFile(
                storedFile.getFilename(),
                storedFile.getSize(),
                storedFile.getContentType(),
                fileContentStorage.load(storedFile.getStoragePath())
        );
    }

    private UserEntity loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
