package com.anilkumar.urlshortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String clientIp) {
        String key = "rate_limit:" + clientIp;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1L) {
            // first request in this window — set the expiry
            redisTemplate.expire(key, WINDOW);
        }

        return currentCount != null && currentCount <= MAX_REQUESTS;
    }
}