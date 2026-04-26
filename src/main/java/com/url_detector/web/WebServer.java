package com.url_detector.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.url_detector.db.DatabaseManager;
import com.url_detector.db.ScanHistoryRepository;
import com.url_detector.db.ScanRecord;
import com.url_detector.model.DetectionResult;
import com.url_detector.model.RiskLevel;
import com.url_detector.service.UrlDetectionService;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class WebServer {

    private final HttpServer server;
    private final UrlDetectionService detectionService;
    private final ScanHistoryRepository repository;

    public WebServer(int port) throws IOException, SQLException {
        DatabaseManager db = new DatabaseManager(Path.of("url_detector.db"));
        db.initializeSchema();

        this.detectionService = new UrlDetectionService();
        this.repository = new ScanHistoryRepository(db);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/analyze", this::handleAnalyze);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/", new StaticHandler());
    }

    public void start() {
        server.start();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleAnalyze(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String url = extractJsonField(body, "url");
        if (url == null || url.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Field 'url' is required\"}");
            return;
        }

        try {
            DetectionResult result = detectionService.analyzeOne(url);
            repository.save(result);
            sendJson(exchange, 200, toResultJson(result));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (SQLException ex) {
            sendJson(exchange, 500, "{\"error\":\"Failed to save scan history\"}");
        }
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("DELETE".equalsIgnoreCase(method)) {
            try {
                repository.deleteAll();
                sendJson(exchange, 200, "{\"status\":\"cleared\"}");
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Failed to clear scan history\"}");
            }
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        int limit = parseIntQuery(query, "limit", 25, 1, 200);
        String risk = parseStringQuery(query, "risk");

        try {
            List<ScanRecord> records;
            if (risk == null || risk.isBlank() || "ALL".equalsIgnoreCase(risk)) {
                records = repository.findRecent(limit);
            } else {
                String normalized = risk.toUpperCase(Locale.ROOT);
                RiskLevel.valueOf(normalized);
                records = repository.searchByRisk(normalized, limit);
            }
            sendJson(exchange, 200, toHistoryJson(records));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"Invalid risk filter\"}");
        } catch (SQLException ex) {
            sendJson(exchange, 500, "{\"error\":\"Failed to load scan history\"}");
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String toResultJson(DetectionResult result) {
        StringBuilder flags = new StringBuilder("[");
        List<String> values = result.getFlags();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                flags.append(',');
            }
            flags.append('"').append(escapeJson(values.get(i))).append('"');
        }
        flags.append(']');

        return "{"
            + "\"url\":\"" + escapeJson(result.getParsedUrl().getRawUrl()) + "\"," 
            + "\"score\":" + result.getTotalScore() + ","
            + "\"riskLevel\":\"" + result.getRiskLevel().name() + "\"," 
            + "\"flags\":" + flags
            + "}";
    }

    private static String toHistoryJson(List<ScanRecord> records) {
        StringBuilder sb = new StringBuilder("{\"records\":[");
        for (int i = 0; i < records.size(); i++) {
            ScanRecord r = records.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{')
                .append("\"id\":").append(r.getId()).append(',')
                .append("\"url\":\"").append(escapeJson(r.getUrl())).append("\",")
                .append("\"score\":").append(r.getScore()).append(',')
                .append("\"riskLevel\":\"").append(escapeJson(r.getRiskLevel())).append("\",")
                .append("\"flagsText\":\"").append(escapeJson(r.getFlagsText())).append("\",")
                .append("\"scannedAt\":\"").append(escapeJson(r.getScannedAt())).append("\"")
                .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String extractJsonField(String body, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyPos = body.indexOf(key);
        if (keyPos < 0) {
            return null;
        }
        int colon = body.indexOf(':', keyPos + key.length());
        if (colon < 0) {
            return null;
        }
        int start = body.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        int end = start + 1;
        while (end < body.length()) {
            if (body.charAt(end) == '"' && body.charAt(end - 1) != '\\') {
                break;
            }
            end++;
        }
        if (end >= body.length()) {
            return null;
        }
        return body.substring(start + 1, end)
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private static int parseIntQuery(String query, String key, int fallback, int min, int max) {
        String raw = parseStringQuery(query, key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw);
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String parseStringQuery(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int pos = pair.indexOf('=');
            if (pos <= 0) {
                continue;
            }
            String currentKey = pair.substring(0, pos);
            if (key.equals(currentKey)) {
                return pair.substring(pos + 1).replace('+', ' ');
            }
        }
        return null;
    }

    private static final class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String resourcePath;
            if ("/".equals(path) || "/index.html".equals(path)) {
                resourcePath = "static/index.html";
            } else if (path.startsWith("/assets/")) {
                resourcePath = "static" + path;
            } else {
                sendBytes(exchange, 404, "text/plain; charset=utf-8",
                    "Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    sendBytes(exchange, 404, "text/plain; charset=utf-8",
                        "Not Found".getBytes(StandardCharsets.UTF_8));
                    return;
                }

                String contentType = resourcePath.endsWith(".css")
                    ? "text/css; charset=utf-8"
                    : resourcePath.endsWith(".js")
                    ? "application/javascript; charset=utf-8"
                    : "text/html; charset=utf-8";
                sendBytes(exchange, 200, contentType, stream.readAllBytes());
            }
        }
    }
}