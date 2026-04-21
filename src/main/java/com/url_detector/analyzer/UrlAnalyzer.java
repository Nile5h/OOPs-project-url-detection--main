package com.url_detector.analyzer;

import com.url_detector.checker.*;
import com.url_detector.model.DetectionResult;
import com.url_detector.model.ParsedUrl;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates all {@link UrlChecker} implementations.
 * Runs each checker, accumulates scores and flags, and returns a
 * {@link DetectionResult}.
 */
public class UrlAnalyzer {

    private final List<UrlChecker> checkers;

    public UrlAnalyzer() {
        this.checkers = List.of(
            new CsvMaliciousListChecker(),
            new StructureChecker(),
            new DomainChecker(),
            new KeywordChecker(),
            new BlacklistChecker(),
            new TyposquatChecker(),
            new CsvSafeListChecker()
        );
    }

    /**
     * Runs every checker against the parsed URL and aggregates results.
     *
     * @param parsedUrl the URL to analyze
     * @return a complete DetectionResult
     */
    public DetectionResult analyze(ParsedUrl parsedUrl) {
        int totalScore = 0;
        List<String> flags = new ArrayList<>();

        for (UrlChecker checker : checkers) {
            List<CheckResult> checkResults = checker.check(parsedUrl);
            for (CheckResult result : checkResults) {
                if (result.hasFlag()) {
                    totalScore += result.getScore();
                    flags.add(result.getReason());
                }
            }
        }

        // Keep score floor at 0 so allowlist deductions never produce negative risk.
        totalScore = Math.max(0, totalScore);

        return new DetectionResult(parsedUrl, totalScore, flags);
    }
}
