package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;
import com.url_detector.util.LevenshteinUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects typosquatting by comparing the registrable domain against a list of
 * popular domains. A Levenshtein distance of 1 or 2 (but not 0 — exact match
 * would be a legitimate domain) triggers a warning.
 */
public class TyposquatChecker implements UrlChecker {

    private static final String RESOURCE_PATH = "/popular_domains.txt";
    private static final int SCORE_DISTANCE_1  = 40;
    private static final int SCORE_DISTANCE_2  = 20;

    private final List<String> popularDomains;

    public TyposquatChecker() {
        this.popularDomains = loadDomains();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();
        String domain = url.getRegistrableDomain().toLowerCase();
        if (domain.isBlank()) return results;

        for (String popular : popularDomains) {
            int dist = LevenshteinUtil.computeDistance(domain, popular);
            if (dist == 1) {
                results.add(new CheckResult(SCORE_DISTANCE_1,
                    "Possible typosquatting: '" + domain + "' is 1 edit away from '" + popular + "'"));
                break; // report the closest match only
            } else if (dist == 2) {
                results.add(new CheckResult(SCORE_DISTANCE_2,
                    "Possible typosquatting: '" + domain + "' is 2 edits away from '" + popular + "'"));
                // don't break — a distance-1 match could follow and is more alarming
            }
        }

        return results;
    }

    private List<String> loadDomains() {
        List<String> domains = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) return domains;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        domains.add(line.toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[TyposquatChecker] Warning: could not load popular domains: " + e.getMessage());
        }
        return domains;
    }
}
