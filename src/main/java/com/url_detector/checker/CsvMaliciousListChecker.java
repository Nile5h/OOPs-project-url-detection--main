package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;
import com.url_detector.util.UrlNormalizationUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uses malicious_phish.csv as a threat-intelligence source.
 * Ignores rows marked benign and scores exact malicious URL matches very highly.
 */
public class CsvMaliciousListChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/malicious_phish.csv";
    private static final int EXACT_URL_SCORE = 120;
    private static final int DOMAIN_SCORE = 90;

    private final Set<String> maliciousUrlKeys;
    private final Set<String> maliciousHosts;

    public CsvMaliciousListChecker() {
        this.maliciousUrlKeys = new HashSet<>();
        this.maliciousHosts = new HashSet<>();
        loadData();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        String rawKey = UrlNormalizationUtil.normalizeUrlKey(url.getRawUrl());
        String hostKey = UrlNormalizationUtil.normalizeHost(url.getHost());

        if (!rawKey.isBlank() && maliciousUrlKeys.contains(rawKey)) {
            results.add(new CheckResult(EXACT_URL_SCORE,
                "Exact URL appears in malicious dataset (malicious_phish.csv)"));
            return results;
        }

        if (!hostKey.isBlank() && maliciousHosts.contains(hostKey)) {
            results.add(new CheckResult(DOMAIN_SCORE,
                "Domain appears in malicious dataset (malicious_phish.csv)"));
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

                    String rawUrl = parts[0].trim();
                    String type = parts[1].trim().toLowerCase();
                    if (type.equals("benign")) {
                        continue;
                    }

                    String normalizedUrlKey = UrlNormalizationUtil.normalizeUrlKey(rawUrl);
                    if (!normalizedUrlKey.isBlank()) {
                        maliciousUrlKeys.add(normalizedUrlKey);
                    }

                    // Domain-level matching is intentionally restricted to phishing-like rows
                    // that are host-only indicators (not full page URLs) to reduce false positives.
                    boolean phishingLike = type.contains("phish") || type.contains("malware") || type.contains("malicious");
                    boolean hostOnlyIndicator = isHostOnlyIndicator(rawUrl);
                    if (phishingLike && hostOnlyIndicator) {
                        String host = UrlNormalizationUtil.extractHost(rawUrl);
                        if (!host.isBlank()) {
                            maliciousHosts.add(host);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CsvMaliciousListChecker] Warning: could not load CSV: " + e.getMessage());
        }
    }

    private boolean isHostOnlyIndicator(String rawUrl) {
        String value = rawUrl.trim().toLowerCase();
        if (value.startsWith("http://")) {
            value = value.substring(7);
        } else if (value.startsWith("https://")) {
            value = value.substring(8);
        }

        // Host-only entries should not include path/query/fragment markers.
        return !value.contains("/") && !value.contains("?") && !value.contains("#");
    }

    private String[] splitCsvLine(String line) {
        // Dataset rows are simple two-column values (url,type).
        return line.split(",", 2);
    }
}
