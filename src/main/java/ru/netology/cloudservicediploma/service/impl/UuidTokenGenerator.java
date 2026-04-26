package ru.netology.cloudservicediploma.service.impl;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.netology.cloudservicediploma.service.TokenGenerator;

@Component
public class UuidTokenGenerator implements TokenGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
