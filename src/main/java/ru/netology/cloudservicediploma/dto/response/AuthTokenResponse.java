package ru.netology.cloudservicediploma.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokenResponse(@JsonProperty("auth-token") String authToken) {
}
