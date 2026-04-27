package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;
import com.url_detector.util.UrlNormalizationUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uses uris.csv (scam/phishing reports) as a direct malicious signal source.
 */
public class UrisScamChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/uris.csv";
    private static final String FALLBACK_FILE = "uris.csv";
    private static final int EXACT_URL_SCORE = 110;
    private static final int HOST_SCORE = 80;

    private final Set<String> maliciousUrlKeys;
    private final Set<String> maliciousHosts;

    public UrisScamChecker() {
        this.maliciousUrlKeys = new HashSet<>();
        this.maliciousHosts = new HashSet<>();
        loadData();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        String rawKey = UrlNormalizationUtil.normalizeUrlKey(url.getRawUrl());
        String host = UrlNormalizationUtil.normalizeHost(url.getHost());

        if (!rawKey.isBlank() && maliciousUrlKeys.contains(rawKey)) {
            results.add(new CheckResult(EXACT_URL_SCORE,
                "Exact URL appears in uris.csv scam/phishing dataset"));
            return results;
        }

        if (!host.isBlank() && maliciousHosts.contains(host)) {
            results.add(new CheckResult(HOST_SCORE,
                "Domain appears in uris.csv scam/phishing dataset"));
        }

        return results;
    }

    private void loadData() {
        try (BufferedReader reader = openReader()) {
            if (reader == null) {
                return;
            }

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }

                String[] parts = line.split(",", 7);
                if (parts.length < 3) {
                    continue;
                }

                String url = parts[1].trim();
                String category = parts[2].trim().toLowerCase();
                if (url.isEmpty()) {
                    continue;
                }

                boolean phishingLike = category.contains("phishing")
                    || category.contains("scam")
                    || category.contains("fake");
                if (!phishingLike) {
                    continue;
                }

                String urlKey = UrlNormalizationUtil.normalizeUrlKey(url);
                if (!urlKey.isBlank()) {
                    maliciousUrlKeys.add(urlKey);
                }

                String host = UrlNormalizationUtil.extractHost(url);
                if (!host.isBlank()) {
                    maliciousHosts.add(host);
                }
            }
        } catch (IOException e) {
            System.err.println("[UrisScamChecker] Warning: could not load uris.csv: " + e.getMessage());
        }
    }

    private BufferedReader openReader() throws IOException {
        InputStream is = getClass().getResourceAsStream(RESOURCE_PATH);
        if (is != null) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        Path fallback = Path.of(FALLBACK_FILE);
        if (Files.exists(fallback)) {
            return Files.newBufferedReader(fallback, StandardCharsets.UTF_8);
        }

        return null;
    }
}