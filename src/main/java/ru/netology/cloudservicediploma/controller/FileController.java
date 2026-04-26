package ru.netology.cloudservicediploma.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservicediploma.dto.request.RenameFileRequest;
import ru.netology.cloudservicediploma.dto.response.FileResponse;
import ru.netology.cloudservicediploma.security.AuthenticatedUser;
import ru.netology.cloudservicediploma.security.CurrentUser;
import ru.netology.cloudservicediploma.service.CloudFileService;
import ru.netology.cloudservicediploma.service.DownloadedFile;
import ru.netology.cloudservicediploma.service.FileMetadata;

@Validated
@RestController
public class FileController {

    private final CloudFileService cloudFileService;

    public FileController(CloudFileService cloudFileService) {
        this.cloudFileService = cloudFileService;
    }

    @GetMapping("/list")
    public List<FileResponse> listFiles(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit
    ) {
        return cloudFileService.listFiles(user, limit)
                .stream()
                .map(this::toFileResponse)
                .toList();
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadFile(
            @CurrentUser AuthenticatedUser user,
            @RequestParam String filename,
            @RequestPart("file") MultipartFile file
    ) {
        cloudFileService.uploadFile(user, filename, file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @CurrentUser AuthenticatedUser user,
            @RequestParam String filename
    ) {
        cloudFileService.deleteFile(user, filename);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @CurrentUser AuthenticatedUser user,
            @RequestParam String filename,
            @Valid @RequestBody RenameFileRequest request
    ) {
        cloudFileService.renameFile(user, filename, request.filename());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @CurrentUser AuthenticatedUser user,
            @RequestParam String filename
    ) {
        DownloadedFile downloadedFile = cloudFileService.downloadFile(user, filename);
        String contentType = downloadedFile.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : downloadedFile.contentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(downloadedFile.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(downloadedFile.filename()).build().toString()
                )
                .body(downloadedFile.resource());
    }

    private FileResponse toFileResponse(FileMetadata fileMetadata) {
        return new FileResponse(fileMetadata.filename(), fileMetadata.size());
    }
}
