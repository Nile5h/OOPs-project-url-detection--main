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
 * Checks whether a URL host or registrable domain exists in blacklist.txt.
 */
public class BlacklistChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/blacklist.txt";
    private static final int MATCH_SCORE = 100;

    private final Set<String> blacklistedDomains;

    public BlacklistChecker() {
        this.blacklistedDomains = loadDomains();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        String host = UrlNormalizationUtil.normalizeHost(url.getHost());
        String registrable = UrlNormalizationUtil.normalizeHost(url.getRegistrableDomain());

        if ((!host.isBlank() && blacklistedDomains.contains(host))
            || (!registrable.isBlank() && blacklistedDomains.contains(registrable))) {
            results.add(new CheckResult(MATCH_SCORE,
                "Domain appears in blacklist (blacklist.txt)"));
        }

        return results;
    }

    private Set<String> loadDomains() {
        Set<String> domains = new HashSet<>();
        try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                return domains;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String value = line.strip();
                    if (value.isEmpty() || value.startsWith("#")) {
                        continue;
                    }

                    String normalized = UrlNormalizationUtil.extractHost(value);
                    if (!normalized.isBlank()) {
                        domains.add(normalized);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[BlacklistChecker] Warning: could not load blacklist: " + e.getMessage());
        }

        return domains;
    }
}
