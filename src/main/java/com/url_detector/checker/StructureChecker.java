package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks for structural anomalies in the URL itself:
 * excessive length, IP-based host, suspicious characters, redirect params, etc.
 */
public class StructureChecker implements UrlChecker {

    private static final Pattern URL_ENCODED  = Pattern.compile("%[0-9A-Fa-f]{2}");
    private static final Pattern REDIRECT_PARAM =
        Pattern.compile("(\\?|&)(url|redirect|next|redir|goto|forward|dest|link)=",
                        Pattern.CASE_INSENSITIVE);

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();
        String raw = url.getRawUrl();

        // ── URL length ──────────────────────────────────────────────────────
        int len = raw.length();
        if (len > 100) {
            results.add(new CheckResult(20, "URL is very long (" + len + " characters)"));
        } else if (len > 75) {
            results.add(new CheckResult(10, "URL is long (" + len + " characters)"));
        }

        // ── IP address as host ──────────────────────────────────────────────
        if (url.isIpBased()) {
            results.add(new CheckResult(30, "Host is an IP address instead of a domain name"));
        }

        // ── Explicit port number ────────────────────────────────────────────
        if (url.getPort() != -1) {
            results.add(new CheckResult(10, "URL specifies a non-standard port: " + url.getPort()));
        }

        // ── @ symbol (credential injection attempt) ─────────────────────────
        if (raw.contains("@")) {
            results.add(new CheckResult(25, "URL contains '@' — possible credential injection"));
        }

        // ── URL-encoded characters ──────────────────────────────────────────
        if (URL_ENCODED.matcher(raw).find()) {
            results.add(new CheckResult(15, "URL contains percent-encoded characters"));
        }

        // ── Excessive hyphens in domain ─────────────────────────────────────
        String host = url.getHost();
        long hyphens = host.chars().filter(c -> c == '-').count();
        if (hyphens > 2) {
            results.add(new CheckResult(10, "Domain contains many hyphens (" + hyphens + ")"));
        }

        // ── Double slashes in path (obfuscation) ────────────────────────────
        String path = url.getPath();
        if (path != null && path.contains("//")) {
            results.add(new CheckResult(10, "URL path contains double slashes (//)"));
        }

        // ── Open redirect parameters ────────────────────────────────────────
        if (REDIRECT_PARAM.matcher(raw).find()) {
            results.add(new CheckResult(20, "URL contains a suspicious redirect parameter"));
        }

        return results;
    }
}
