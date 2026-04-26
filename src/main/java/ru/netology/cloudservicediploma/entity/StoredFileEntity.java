package ru.netology.cloudservicediploma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import ru.netology.cloudservicediploma.entity.UserEntity;

@Entity
@Table(name = "stored_file")
public class StoredFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long size;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoredFileEntity() {
    }

    public StoredFileEntity(
            UserEntity user,
            String filename,
            long size,
            String contentType,
            String storagePath,
            Instant uploadedAt,
            Instant updatedAt
    ) {
        this.user = user;
        this.filename = filename;
        this.size = size;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void rename(String filename, String storagePath, Instant updatedAt) {
        this.filename = filename;
        this.storagePath = storagePath;
        this.updatedAt = updatedAt;
    }
}
