package ru.netology.cloudservicediploma.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.netology.cloudservicediploma.entity.SessionTokenEntity;

public interface SessionTokenRepository extends JpaRepository<SessionTokenEntity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SessionTokenEntity> findByTokenHashAndActiveTrue(String tokenHash);

    @Modifying
    @Query("delete from SessionTokenEntity sessionToken where sessionToken.expiresAt < :expiresAt")
    int deleteExpiredBefore(@Param("expiresAt") Instant expiresAt);
}
