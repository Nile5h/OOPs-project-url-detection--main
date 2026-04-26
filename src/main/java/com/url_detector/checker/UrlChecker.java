package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;

import java.util.List;

/**
 * Contract for all URL analysis checkers.
 * Each checker inspects a {@link ParsedUrl} and returns a list of
 * {@link CheckResult} objects — one per signal found.
 */
public interface UrlChecker {

    /**
     * Analyzes the given parsed URL and returns all detected signals.
     *
     * @param url the parsed URL to analyze
     * @return a non-null list of CheckResults (may be empty if nothing found)
     */
    List<CheckResult> check(ParsedUrl url);
}
