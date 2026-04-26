package com.url_detector.util;

import java.net.URI;

/**
 * Utility helpers for creating consistent URL/domain keys used in CSV lookups.
 */
public final class UrlNormalizationUtil {

    private UrlNormalizationUtil() {
    }

    public static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase();
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }

    public static String extractHost(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String value = raw.trim();
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }

        String candidate = value;
        if (!candidate.contains("://")) {
            candidate = "http://" + candidate;
        }

        try {
            URI uri = new URI(candidate);
            String host = uri.getHost();
            if (host != null && !host.isBlank()) {
                return normalizeHost(host);
            }
        } catch (Exception ignored) {
            // Fallback below
        }

        int slash = value.indexOf('/');
        String fallback = slash >= 0 ? value.substring(0, slash) : value;
        int question = fallback.indexOf('?');
        if (question >= 0) {
            fallback = fallback.substring(0, question);
        }
        int hash = fallback.indexOf('#');
        if (hash >= 0) {
            fallback = fallback.substring(0, hash);
        }
        return normalizeHost(fallback);
    }

    public static String normalizeUrlKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String value = raw.trim().toLowerCase();
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }

        // Keep original scheme if present, otherwise prepend for stable parsing.
        String candidate = value.contains("://") ? value : "http://" + value;
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
            String host = normalizeHost(uri.getHost());
            String path = uri.getPath() != null ? uri.getPath() : "";
            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";

            String normalized = scheme + "://" + host + path + query;
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        } catch (Exception ignored) {
            String fallback = value;
            if (!fallback.contains("://")) {
                fallback = "http://" + fallback;
            }
            if (fallback.endsWith("/")) {
                fallback = fallback.substring(0, fallback.length() - 1);
            }
            return fallback;
        }
    }
}
