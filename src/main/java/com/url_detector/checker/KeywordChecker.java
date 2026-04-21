package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans the full URL string for keywords commonly found in phishing pages:
 * account-related actions, financial brands, social-engineering lures.
 * Score is capped to avoid inflating the total unfairly.
 */
public class KeywordChecker implements UrlChecker {

    private static final int SCORE_PER_KEYWORD = 10;
    private static final int MAX_SCORE         = 30;

    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
        "login", "signin", "sign-in", "log-in",
        "account", "secure", "security",
        "verify", "verification", "validate", "confirm",
        "update", "upgrade", "alert", "warning",
        "bank", "banking", "billing", "invoice", "payment",
        "password", "passwd", "credential", "username",
        "wallet", "crypto", "bitcoin", "token",
        "free", "prize", "winner", "lucky", "congratulations",
        "urgent", "suspended", "locked", "blocked", "limited"
    );

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();
        String lowerUrl = url.getRawUrl().toLowerCase();

        int totalScore = 0;
        List<String> found = new ArrayList<>();

        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (lowerUrl.contains(keyword)) {
                found.add(keyword);
                totalScore += SCORE_PER_KEYWORD;
                if (totalScore >= MAX_SCORE) {
                    totalScore = MAX_SCORE;
                    break;
                }
            }
        }

        if (!found.isEmpty()) {
            results.add(new CheckResult(totalScore,
                "Suspicious keyword(s) found in URL: " + String.join(", ", found)));
        }

        return results;
    }
}
