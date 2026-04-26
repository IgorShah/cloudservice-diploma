package ru.netology.cloudservicediploma.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservicediploma.dto.response.FileResponse;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;

public interface CloudFileService {

    List<FileResponse> listFiles(AuthenticatedUser user, int limit);

    void uploadFile(AuthenticatedUser user, String filename, MultipartFile file);

    void deleteFile(AuthenticatedUser user, String filename);

    void renameFile(AuthenticatedUser user, String sourceFilename, String targetFilename);

    DownloadedFile downloadFile(AuthenticatedUser user, String filename);
}
