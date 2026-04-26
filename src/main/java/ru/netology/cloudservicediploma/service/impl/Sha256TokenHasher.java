package ru.netology.cloudservicediploma.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import ru.netology.cloudservicediploma.service.TokenHasher;

@Component
public class Sha256TokenHasher implements TokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public String hash(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Token hash algorithm is not available", exception);
        }
    }
}
