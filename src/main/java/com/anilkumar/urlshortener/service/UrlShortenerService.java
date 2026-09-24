package com.anilkumar.urlshortener.service;

import com.anilkumar.urlshortener.model.UrlMapping;
import com.anilkumar.urlshortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
@Service
public class UrlShortenerService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public String createShortUrl(String originalUrl) {
        String shortCode = generateUniqueCode();

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);
        mapping.setCreatedAt(LocalDateTime.now());

        repository.save(mapping);

        // Pre-populate cache so first redirect is already fast
        redisTemplate.opsForValue().set(shortCode, originalUrl, Duration.ofHours(24));

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        // 1. Check Redis first
        String cachedUrl = redisTemplate.opsForValue().get(shortCode);
        if (cachedUrl != null) {
            incrementClickCountAsync(shortCode);
            return cachedUrl;
        }

        // 2. Cache miss — fall back to database
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // 3. Populate cache for next time
        redisTemplate.opsForValue().set(shortCode, mapping.getOriginalUrl(), Duration.ofHours(24));

        mapping.setClickCount(mapping.getClickCount() + 1);
        repository.save(mapping);

        return mapping.getOriginalUrl();
    }

    private void incrementClickCountAsync(String shortCode) {
        repository.findByShortCode(shortCode).ifPresent(mapping -> {
            mapping.setClickCount(mapping.getClickCount() + 1);
            repository.save(mapping);
        });
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (repository.findByShortCode(code).isPresent());
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
    public Optional<Map<String, Object>> getStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(mapping -> Map.of(
                        "shortCode", mapping.getShortCode(),
                        "originalUrl", mapping.getOriginalUrl(),
                        "clickCount", mapping.getClickCount(),
                        "createdAt", mapping.getCreatedAt()
                ));
    }
}