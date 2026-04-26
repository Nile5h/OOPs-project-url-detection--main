package com.url_detector.parser;

import com.url_detector.model.ParsedUrl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses a raw URL string into a {@link ParsedUrl} using java.net.URI.
 */
public class UrlParser {

    // Matches IPv4 addresses
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(\\d{1,3}\\.){3}\\d{1,3}$"
    );

    /**
     * Parses the given URL string.
     *
     * @param rawUrl the URL to parse
     * @return a populated ParsedUrl
     * @throws IllegalArgumentException if the URL cannot be parsed
     */
    public ParsedUrl parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }

        String normalized = rawUrl.trim();

        // Strip UTF-8 BOM if accidentally present (e.g. when reading from BOM-encoded files)
        if (normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }

        // Keep the clean (BOM-stripped) URL for display purposes
        String cleanUrl = normalized;

        // Prepend scheme if missing so java.net.URI can parse the host
        if (!normalized.contains("://")) {
            normalized = "http://" + normalized;
        }

        URI uri;
        try {
            uri = new URI(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl);
        }

        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
        String host   = uri.getHost()   != null ? uri.getHost().toLowerCase()   : "";
        String path   = uri.getPath()   != null ? uri.getPath()                 : "";
        String query  = uri.getQuery()  != null ? uri.getQuery()                : "";
        int    port   = uri.getPort(); // -1 if absent

        boolean ipBased = IP_PATTERN.matcher(host).matches();

        // Split host into labels
        String[] labels = host.split("\\.");
        String tld = "";
        String registrableDomain = "";
        List<String> subdomains = new ArrayList<>();

        if (labels.length >= 2 && !ipBased) {
            tld = labels[labels.length - 1];
            registrableDomain = labels[labels.length - 2] + "." + tld;
            // Everything before the last two labels is a subdomain
            if (labels.length > 2) {
                subdomains = Arrays.asList(Arrays.copyOf(labels, labels.length - 2));
            }
        } else if (ipBased) {
            registrableDomain = host;
        } else {
            registrableDomain = host;
        }

        return new ParsedUrl(cleanUrl, scheme, host, registrableDomain, tld,
                     subdomains, path, query, port, ipBased);
    }
}
