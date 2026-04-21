package com.url_detector.service;

import com.url_detector.analyzer.UrlAnalyzer;
import com.url_detector.model.DetectionResult;
import com.url_detector.model.ParsedUrl;
import com.url_detector.model.RiskLevel;
import com.url_detector.parser.UrlParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Shared application service used by UI/controllers to run URL analysis.
 */
public class UrlDetectionService {

    private final UrlParser parser;
    private final UrlAnalyzer analyzer;

    public UrlDetectionService() {
        this.parser = new UrlParser();
        this.analyzer = new UrlAnalyzer();
    }

    public DetectionResult analyzeOne(String rawUrl) {
        ParsedUrl parsed = parser.parse(rawUrl);
        return analyzer.analyze(parsed);
    }

    public List<DetectionResult> analyzeBatch(List<String> rawUrls) {
        List<DetectionResult> results = new ArrayList<>();
        for (String rawUrl : rawUrls) {
            try {
                results.add(analyzeOne(rawUrl));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed rows in batch mode.
            }
        }
        return results;
    }

    public List<DetectionResult> analyzeFromFile(Path path) throws IOException {
        List<String> urls = Files.readAllLines(path, StandardCharsets.UTF_8)
            .stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .toList();
        return analyzeBatch(urls);
    }

    public Map<RiskLevel, Long> summarize(List<DetectionResult> results) {
        Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            counts.put(level, 0L);
        }
        for (DetectionResult result : results) {
            counts.merge(result.getRiskLevel(), 1L, Long::sum);
        }
        return counts;
    }
}
