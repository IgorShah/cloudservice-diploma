package ru.netology.cloudservicediploma.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudservicediploma.entity.StoredFileEntity;

public interface StoredFileRepository extends JpaRepository<StoredFileEntity, Long> {

    Page<StoredFileEntity> findAllByUserIdOrderByUploadedAtDesc(Long userId, Pageable pageable);

    Optional<StoredFileEntity> findByUserIdAndFilename(Long userId, String filename);

    boolean existsByUserIdAndFilename(Long userId, String filename);
}
