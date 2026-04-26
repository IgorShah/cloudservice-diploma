package ru.netology.cloudservicediploma.service;

public interface TokenHasher {

    String hash(String token);
}
