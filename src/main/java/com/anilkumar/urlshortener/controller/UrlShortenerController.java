package com.anilkumar.urlshortener.controller;

import com.anilkumar.urlshortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import com.anilkumar.urlshortener.service.UrlShortenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
public class UrlShortenerController {

    @Autowired
    private UrlShortenerService service;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private RateLimiterService rateLimiterService;

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimiterService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Try again later."));
        }

        String originalUrl = request.get("url");
        String shortCode = service.createShortUrl(originalUrl);
        String shortUrl = baseUrl + "/" + shortCode;
        return ResponseEntity.ok(Map.of("shortUrl", shortUrl));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.getOriginalUrl(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}