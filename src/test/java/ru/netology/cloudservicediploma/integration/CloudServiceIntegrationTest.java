package ru.netology.cloudservicediploma.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cloudservicediploma.repository.SessionTokenRepository;
import ru.netology.cloudservicediploma.repository.StoredFileRepository;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class CloudServiceIntegrationTest {

    private static final Path STORAGE_ROOT = createTempStorage();

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cloudservice")
            .withUsername("cloudservice")
            .withPassword("cloudservice");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("app.storage.root-path", () -> STORAGE_ROOT.toString());
    }

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private SessionTokenRepository sessionTokenRepository;
    @Autowired
    private StoredFileRepository storedFileRepository;

    @AfterEach
    void cleanUp() throws IOException {
        storedFileRepository.deleteAll();
        sessionTokenRepository.deleteAll();
        clearStorageDirectory();
    }

    @Test
    void loginReturnsAuthToken() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "user@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").isNotEmpty());
    }

    @Test
    void fileLifecycleWorksEndToEnd() throws Exception {
        String authToken = loginAndGetToken();

        mockMvc.perform(multipart("/file")
                        .file("file", "hello world".getBytes())
                        .param("filename", "notes.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("notes.txt"))
                .andExpect(jsonPath("$[0].size").value(11));

        mockMvc.perform(put("/file")
                        .param("filename", "notes.txt")
                        .header("auth-token", authToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "renamed.txt"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("renamed.txt")))
                .andExpect(content().bytes("hello world".getBytes()));

        mockMvc.perform(delete("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void logoutAcceptsBearerAuthTokenHeader() throws Exception {
        String authToken = loginAndGetToken();

        mockMvc.perform(post("/logout")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", authToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingRequestParameterReturnsBadRequestMessage() throws Exception {
        String authToken = loginAndGetToken();

        mockMvc.perform(delete("/file")
                        .header("auth-token", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error input data"));
    }

    @Test
    void unreadableJsonReturnsBadRequestMessage() throws Exception {
        String authToken = loginAndGetToken();

        mockMvc.perform(put("/file")
                        .param("filename", "notes.txt")
                        .header("auth-token", authToken)
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error input data"));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "user@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("auth-token").asText();
    }

    private static Path createTempStorage() {
        try {
            return Files.createTempDirectory("cloudservice-storage-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void clearStorageDirectory() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }

        try (var stream = Files.walk(STORAGE_ROOT)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(STORAGE_ROOT))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
