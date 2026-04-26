package ru.netology.cloudservicediploma.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.netology.cloudservicediploma.entity.StoredFileEntity;
import ru.netology.cloudservicediploma.entity.UserEntity;
import ru.netology.cloudservicediploma.exception.FileAlreadyExistsException;
import ru.netology.cloudservicediploma.repository.StoredFileRepository;
import ru.netology.cloudservicediploma.repository.UserRepository;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;
import ru.netology.cloudservicediploma.storage.FileContentStorage;

@ExtendWith(MockitoExtension.class)
class DefaultCloudFileServiceTest {

    @Mock
    private StoredFileRepository storedFileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileContentStorage fileContentStorage;

    private DefaultCloudFileService cloudFileService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T10:15:30Z"), ZoneOffset.UTC);
        cloudFileService = new DefaultCloudFileService(storedFileRepository, userRepository, fileContentStorage, clock);
    }

    @Test
    void uploadFileStoresMetadataAndBinaryContent() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@example.com");
        UserEntity persistedUser = new UserEntity("user@example.com", "hash");
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        when(storedFileRepository.existsByUserIdAndFilename(1L, "notes.txt")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(persistedUser));
        when(fileContentStorage.save(eq(1L), eq("notes.txt"), any())).thenReturn(Path.of("1", "stored.txt").toString());

        cloudFileService.uploadFile(user, "notes.txt", multipartFile);

        verify(fileContentStorage).save(eq(1L), eq("notes.txt"), any());
        verify(storedFileRepository).save(any(StoredFileEntity.class));
    }

    @Test
    void renameFileRejectsDuplicateTargetName() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@example.com");
        when(storedFileRepository.existsByUserIdAndFilename(1L, "renamed.txt")).thenReturn(true);

        assertThatThrownBy(() -> cloudFileService.renameFile(user, "notes.txt", "renamed.txt"))
                .isInstanceOf(FileAlreadyExistsException.class)
                .hasMessage("File already exists");
    }

    @Test
    void deleteFileRemovesMetadataAndStoredContent() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@example.com");
        UserEntity persistedUser = new UserEntity("user@example.com", "hash");
        StoredFileEntity storedFile = new StoredFileEntity(
                persistedUser,
                "notes.txt",
                5L,
                "text/plain",
                Path.of("1", "stored.txt").toString(),
                Instant.parse("2026-04-25T10:15:30Z"),
                Instant.parse("2026-04-25T10:15:30Z")
        );
        when(storedFileRepository.findByUserIdAndFilename(1L, "notes.txt")).thenReturn(Optional.of(storedFile));

        cloudFileService.deleteFile(user, "notes.txt");

        verify(fileContentStorage).delete(Path.of("1", "stored.txt").toString());
        verify(storedFileRepository).delete(storedFile);
    }
}
