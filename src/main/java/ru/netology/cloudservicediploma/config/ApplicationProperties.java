package ru.netology.cloudservicediploma.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @Valid @NotNull Auth auth,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Cors cors,
        @Valid @NotEmpty List<SeedUser> users
) {

    public record Auth(
            @NotNull Duration sessionTtl,
            @Positive long expiredTokenCleanupDelayMs
    ) {
    }

    public record Storage(@NotNull Path rootPath) {
    }

    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }

    public record SeedUser(@NotBlank String login, @NotBlank String password) {
    }
}
