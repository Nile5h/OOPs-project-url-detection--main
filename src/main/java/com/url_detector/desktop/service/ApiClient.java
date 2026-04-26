package com.url_detector.desktop.service;

import com.url_detector.desktop.model.AnalyzeResponse;
import com.url_detector.desktop.model.HealthResponse;
import com.url_detector.desktop.model.HistoryRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for communication with the URL Detector backend API.
 */
public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Check if the backend is healthy and responding.
     */
    public static HealthResponse checkHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            long timestamp = System.currentTimeMillis();
            return new HealthResponse("ok", timestamp);
        } else {
            return new HealthResponse("error", System.currentTimeMillis());
        }
    }

    /**
     * Analyze a URL and return risk assessment.
     */
    public static AnalyzeResponse analyzeUrl(String url) throws Exception {
        String jsonBody = "{\"url\":\"" + escapeJson(url) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/analyze"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublisher.fromString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseAnalyzeResponse(response.body());
        } else {
            String error = response.body().contains("error") ? 
                    extractJsonField(response.body(), "error") : 
                    "Unknown error";
            throw new RuntimeException("Analyze failed: " + error);
        }
    }

    /**
     * Get scan history, optionally filtered by risk level.
     */
    public static List<HistoryRecord> getHistory(String riskFilter) throws Exception {
        String url = BASE_URL + "/history";
        if (riskFilter != null && !riskFilter.isEmpty() && !riskFilter.equals("all")) {
            url += "?risk=" + riskFilter;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseHistoryResponse(response.body());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Clear all scan history.
     */
    public static boolean clearHistory() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/history"))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    // JSON Parsing Utilities

    private static AnalyzeResponse parseAnalyzeResponse(String json) {
        AnalyzeResponse response = new AnalyzeResponse();
        response.url = extractJsonField(json, "url");
        response.risk = extractJsonField(json, "risk");
        response.score = parseInt(extractJsonField(json, "score"));
        response.flags = parseJsonArray(extractJsonArray(json, "flags"));
        response.timestamp = System.currentTimeMillis();
        return response;
    }

    private static List<HistoryRecord> parseHistoryResponse(String json) {
        List<HistoryRecord> records = new ArrayList<>();
        // Extract array from response
        String arrayStr = extractJsonArray(json, "records");
        if (arrayStr == null || arrayStr.isEmpty()) {
            return records;
        }

        // Split records by closing brace + comma pattern
        String[] recordsStr = arrayStr.split("\\},\\s*\\{");
        for (String recordStr : recordsStr) {
            // Clean up the braces
            recordStr = recordStr.replaceAll("[{}]", "");
            int id = parseInt(extractJsonField("{" + recordStr + "}", "id"));
            String url = extractJsonField("{" + recordStr + "}", "url");
            String risk = extractJsonField("{" + recordStr + "}", "risk");
            int score = parseInt(extractJsonField("{" + recordStr + "}", "score"));
            List<String> flags = parseJsonArray(extractJsonArray("{" + recordStr + "}", "flags"));
            long timestamp = parseLong(extractJsonField("{" + recordStr + "}", "timestamp"));

            records.add(new HistoryRecord(id, url, risk, score, flags, timestamp));
        }
        return records;
    }

    private static String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String extractJsonArray(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*(\\[[^\\]]*\\])";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "[]";
    }

    private static List<String> parseJsonArray(String arrayStr) {
        List<String> result = new ArrayList<>();
        // Remove brackets
        arrayStr = arrayStr.replaceAll("[\\[\\]]", "").trim();
        if (arrayStr.isEmpty()) {
            return result;
        }

        // Split by comma and clean quotes
        String[] items = arrayStr.split(",");
        for (String item : items) {
            String cleaned = item.replaceAll("[\"\\s]", "").trim();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
