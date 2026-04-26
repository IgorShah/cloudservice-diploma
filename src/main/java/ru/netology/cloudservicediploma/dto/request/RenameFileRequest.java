package ru.netology.cloudservicediploma.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RenameFileRequest(@JsonProperty("filename") @NotBlank String filename) {
}
