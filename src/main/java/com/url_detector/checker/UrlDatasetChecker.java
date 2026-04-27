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
 * Uses url_dataset.csv to reduce risk for known legitimate hosts.
 */
public class UrlDatasetChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/url_dataset.csv";
    private static final String FALLBACK_FILE = "url_dataset.csv";
    private static final int EXACT_HOST_REDUCTION = -25;
    private static final int REGISTRABLE_REDUCTION = -15;

    private final Set<String> legitimateHosts;

    public UrlDatasetChecker() {
        this.legitimateHosts = new HashSet<>();
        loadData();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        String host = UrlNormalizationUtil.normalizeHost(url.getHost());
        String registrable = UrlNormalizationUtil.normalizeHost(url.getRegistrableDomain());

        if (!host.isBlank() && legitimateHosts.contains(host)) {
            results.add(new CheckResult(EXACT_HOST_REDUCTION,
                "Domain appears in url_dataset.csv legitimate list"));
            return results;
        }

        if (!registrable.isBlank() && legitimateHosts.contains(registrable)) {
            results.add(new CheckResult(REGISTRABLE_REDUCTION,
                "Base domain appears in url_dataset.csv legitimate list"));
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

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split(",", 2);
                if (parts.length < 2) {
                    continue;
                }

                String type = parts[1].trim().toLowerCase();
                if (!"legitimate".equals(type)) {
                    continue;
                }

                String host = UrlNormalizationUtil.extractHost(parts[0].trim());
                if (!host.isBlank()) {
                    legitimateHosts.add(host);
                }
            }
        } catch (IOException e) {
            System.err.println("[UrlDatasetChecker] Warning: could not load url_dataset.csv: " + e.getMessage());
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