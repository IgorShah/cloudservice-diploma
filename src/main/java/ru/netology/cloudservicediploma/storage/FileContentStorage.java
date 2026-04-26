package ru.netology.cloudservicediploma.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface FileContentStorage {

    String save(Long userId, String filename, InputStream content);

    String move(Long userId, String currentStoragePath, String newFilename);

    void delete(String storagePath);

    Resource load(String storagePath);
}
