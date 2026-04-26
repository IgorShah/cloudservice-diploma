package ru.netology.cloudservicediploma.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudservicediploma.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);
}
