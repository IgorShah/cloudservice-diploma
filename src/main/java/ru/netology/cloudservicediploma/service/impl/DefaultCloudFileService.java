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
import ru.netology.cloudservicediploma.entity.StoredFileEntity;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.exception.CloudFileNotFoundException;
import ru.netology.cloudservicediploma.exception.EmptyFileException;
import ru.netology.cloudservicediploma.exception.FileAlreadyExistsException;
import ru.netology.cloudservicediploma.exception.StorageException;
import ru.netology.cloudservicediploma.exception.UserNotFoundException;
import ru.netology.cloudservicediploma.repository.StoredFileRepository;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.service.CloudFileService;
import ru.netology.cloudservicediploma.service.DownloadedFile;
import ru.netology.cloudservicediploma.service.FileNameSanitizer;
import ru.netology.cloudservicediploma.service.FileMetadata;
import ru.netology.cloudservicediploma.storage.FileContentStorage;

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
    public List<FileMetadata> listFiles(Long userId, int limit) {
        return storedFileRepository.findAllByUserIdOrderByUploadedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(file -> new FileMetadata(file.getFilename(), file.getSize()))
                .toList();
    }

    @Override
    @Transactional
    public void uploadFile(Long userId, String filename, MultipartFile file) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        if (storedFileRepository.existsByUserIdAndFilename(userId, sanitizedFilename)) {
            throw new FileAlreadyExistsException();
        }

        UserEntity persistedUser = loadUser(userId);
        try (InputStream content = file.getInputStream()) {
            String storagePath = fileContentStorage.save(userId, sanitizedFilename, content);
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
    public void deleteFile(Long userId, String filename) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(userId, sanitizedFilename)
                .orElseThrow(CloudFileNotFoundException::new);
        fileContentStorage.delete(storedFile.getStoragePath());
        storedFileRepository.delete(storedFile);
    }

    @Override
    @Transactional
    public void renameFile(Long userId, String sourceFilename, String targetFilename) {
        String sanitizedSourceFilename = FileNameSanitizer.sanitize(sourceFilename);
        String sanitizedTargetFilename = FileNameSanitizer.sanitize(targetFilename);
        if (sanitizedSourceFilename.equals(sanitizedTargetFilename)) {
            return;
        }
        if (storedFileRepository.existsByUserIdAndFilename(userId, sanitizedTargetFilename)) {
            throw new FileAlreadyExistsException();
        }

        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(userId, sanitizedSourceFilename)
                .orElseThrow(CloudFileNotFoundException::new);
        String newStoragePath = fileContentStorage.move(userId, storedFile.getStoragePath(), sanitizedTargetFilename);
        storedFile.rename(sanitizedTargetFilename, newStoragePath, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile downloadFile(Long userId, String filename) {
        String sanitizedFilename = FileNameSanitizer.sanitize(filename);
        StoredFileEntity storedFile = storedFileRepository.findByUserIdAndFilename(userId, sanitizedFilename)
                .orElseThrow(CloudFileNotFoundException::new);

        return new DownloadedFile(
                storedFile.getFilename(),
                storedFile.getSize(),
                storedFile.getContentType(),
                fileContentStorage.load(storedFile.getStoragePath())
        );
    }

    private UserEntity loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
