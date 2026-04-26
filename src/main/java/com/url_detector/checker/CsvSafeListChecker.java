package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;
import com.url_detector.util.UrlNormalizationUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses safe_sites_df.csv as an allowlist signal to reduce false positives.
 */
public class CsvSafeListChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/safe_sites_df.csv";
    private static final int EXACT_HOST_REDUCTION = -25;
    private static final int REGISTRABLE_REDUCTION = -15;

    private final Map<String, String> safeDomainCategories;

    public CsvSafeListChecker() {
        this.safeDomainCategories = new HashMap<>();
        loadData();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        String host = UrlNormalizationUtil.normalizeHost(url.getHost());
        String registrable = UrlNormalizationUtil.normalizeHost(url.getRegistrableDomain());

        if (!host.isBlank() && safeDomainCategories.containsKey(host)) {
            String category = safeDomainCategories.get(host);
            results.add(new CheckResult(EXACT_HOST_REDUCTION,
                "Domain found in safe dataset (" + category + ")"));
            return results;
        }

        if (!registrable.isBlank() && safeDomainCategories.containsKey(registrable)) {
            String category = safeDomainCategories.get(registrable);
            results.add(new CheckResult(REGISTRABLE_REDUCTION,
                "Base domain found in safe dataset (" + category + ")"));
        }

        return results;
    }

    private void loadData() {
        try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) {
                        first = false;
                        continue;
                    }

                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    String[] parts = splitCsvLine(trimmed);
                    if (parts.length < 2) {
                        continue;
                    }

                    String domain = UrlNormalizationUtil.extractHost(parts[0].trim());
                    String category = parts[1].trim();
                    if (!domain.isBlank()) {
                        safeDomainCategories.putIfAbsent(domain, category);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CsvSafeListChecker] Warning: could not load CSV: " + e.getMessage());
        }
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", 2);
    }
}
