package com.url_detector.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url_detector.desktop.model.AnalyzeResponse;
import com.url_detector.desktop.model.HealthResponse;
import com.url_detector.desktop.model.HistoryRecord;
import com.url_detector.desktop.model.HistoryResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean checkHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return false;
        }

        HealthResponse health = objectMapper.readValue(response.body(), HealthResponse.class);
        return "ok".equalsIgnoreCase(health.getStatus());
    }

    public AnalyzeResponse analyzeUrl(String url) throws IOException, InterruptedException {
        Map<String, String> payload = Map.of("url", url);
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/analyze"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(extractError(response.body(), "Scan failed"));
        }

        return objectMapper.readValue(response.body(), AnalyzeResponse.class);
    }

    public List<HistoryRecord> loadHistory(String riskFilter, int limit) throws IOException, InterruptedException {
        String query;
        if (riskFilter == null || riskFilter.isBlank() || "ALL".equalsIgnoreCase(riskFilter)) {
            query = "?limit=" + limit;
        } else {
            query = "?limit=" + limit + "&risk=" + URLEncoder.encode(riskFilter, StandardCharsets.UTF_8);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/history" + query))
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(extractError(response.body(), "Unable to load history"));
        }

        HistoryResponse history = objectMapper.readValue(response.body(), HistoryResponse.class);
        return history.getRecords();
    }

    public void clearHistory() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/history"))
            .DELETE()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(extractError(response.body(), "Unable to clear history"));
        }
    }

    private String extractError(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        try {
            Map<String, Object> decoded = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
            Object error = decoded.get("error");
            if (error != null) {
                return String.valueOf(error);
            }
        } catch (Exception ignored) {
            return fallback;
        }

        return fallback;
    }
}
