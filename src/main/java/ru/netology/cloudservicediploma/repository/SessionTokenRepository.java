package ru.netology.cloudservicediploma.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudservicediploma.entity.SessionTokenEntity;

public interface SessionTokenRepository extends JpaRepository<SessionTokenEntity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SessionTokenEntity> findByTokenAndActiveTrue(String token);
}
