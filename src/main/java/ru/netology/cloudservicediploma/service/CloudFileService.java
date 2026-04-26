package ru.netology.cloudservicediploma.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CloudFileService {

    List<FileMetadata> listFiles(Long userId, int limit);

    void uploadFile(Long userId, String filename, MultipartFile file);

    void deleteFile(Long userId, String filename);

    void renameFile(Long userId, String sourceFilename, String targetFilename);

    DownloadedFile downloadFile(Long userId, String filename);
}
