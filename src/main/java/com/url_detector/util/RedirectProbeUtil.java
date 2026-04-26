package com.url_detector.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Performs bounded HTTP redirect probing for a URL.
 */
public final class RedirectProbeUtil {

    private static final int MAX_HOPS = 8;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private RedirectProbeUtil() {
    }

    public static RedirectProbeResult probe(String rawUrl) {
        URI start = toHttpUri(rawUrl);
        if (start == null) {
            return RedirectProbeResult.unavailable("Unsupported or invalid URL");
        }

        List<URI> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        URI current = start;
        String startHost = normalizedHost(start.getHost());
        if (startHost.isBlank()) {
            return RedirectProbeResult.unavailable("URL host missing");
        }
        String startRegistrable = registrableDomain(startHost);

        visited.add(normalizeForLoop(start));
        int domainChanges = 0;
        boolean anyRedirect = false;

        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            if (isPrivateOrLocalHost(current.getHost())) {
                return RedirectProbeResult.blocked(chain, current, domainChanges,
                    "Redirect target points to local/private network host");
            }

            chain.add(current);
            HttpResponse<Void> response;
            try {
                response = sendWithHeadFallback(current);
            } catch (IOException e) {
                return RedirectProbeResult.failed(chain, current, anyRedirect, domainChanges,
                    "Network error during redirect probe: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return RedirectProbeResult.failed(chain, current, anyRedirect, domainChanges,
                    "Redirect probe interrupted");
            }

            int status = response.statusCode();
            if (!isRedirectStatus(status)) {
                return RedirectProbeResult.completed(chain, current, anyRedirect, domainChanges);
            }

            anyRedirect = true;
            Optional<String> location = firstHeader(response.headers(), "location");
            if (location.isEmpty() || location.get().isBlank()) {
                return RedirectProbeResult.failed(chain, current, anyRedirect, domainChanges,
                    "Redirect response without Location header");
            }

            URI next = resolveLocation(current, location.get());
            if (next == null) {
                return RedirectProbeResult.failed(chain, current, anyRedirect, domainChanges,
                    "Malformed redirect Location header");
            }

            String nextHost = normalizedHost(next.getHost());
            String nextRegistrable = registrableDomain(nextHost);
            if (!nextRegistrable.isBlank() && !nextRegistrable.equals(startRegistrable)) {
                domainChanges++;
            }

            String marker = normalizeForLoop(next);
            if (visited.contains(marker)) {
                chain.add(next);
                return RedirectProbeResult.loop(chain, next, domainChanges);
            }

            visited.add(marker);
            current = next;

            if (hop == MAX_HOPS) {
                return RedirectProbeResult.maxHops(chain, current, domainChanges);
            }
        }

        return RedirectProbeResult.maxHops(chain, current, domainChanges);
    }

    private static HttpResponse<Void> sendWithHeadFallback(URI uri) throws IOException, InterruptedException {
        HttpRequest head = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", "URL-Detector/1.0")
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<Void> response = CLIENT.send(head, HttpResponse.BodyHandlers.discarding());
        int code = response.statusCode();
        if (code == 405 || code == 501) {
            HttpRequest get = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "URL-Detector/1.0")
                .GET()
                .build();
            return CLIENT.send(get, HttpResponse.BodyHandlers.discarding());
        }

        return response;
    }

    private static Optional<String> firstHeader(HttpHeaders headers, String name) {
        return headers.firstValue(name);
    }

    private static URI resolveLocation(URI current, String location) {
        try {
            URI resolved = current.resolve(location.trim());
            return stripFragment(resolved);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static URI stripFragment(URI uri) {
        try {
            return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                null
            );
        } catch (URISyntaxException ignored) {
            return uri;
        }
    }

    private static URI toHttpUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String value = rawUrl.trim();
        if (!value.contains("://")) {
            value = "http://" + value;
        }

        try {
            URI uri = stripFragment(new URI(value));
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return uri;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeForLoop(URI uri) {
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = normalizedHost(uri.getHost());
        int port = uri.getPort();
        String path = uri.getPath() == null ? "" : uri.getPath();
        String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
        return scheme + "://" + host + ":" + port + path + query;
    }

    private static String normalizedHost(String host) {
        if (host == null) {
            return "";
        }
        return host.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isRedirectStatus(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String registrableDomain(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String[] labels = host.split("\\.");
        if (labels.length < 2) {
            return host;
        }
        return labels[labels.length - 2] + "." + labels[labels.length - 1];
    }

    private static boolean isPrivateOrLocalHost(String host) {
        String h = normalizedHost(host);
        if (h.isBlank()) {
            return true;
        }

        if ("localhost".equals(h) || h.endsWith(".localhost")) {
            return true;
        }

        try {
            InetAddress address = InetAddress.getByName(h);
            return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
        } catch (Exception ignored) {
            // DNS failures are handled later during HTTP probing.
            return false;
        }
    }

    public static final class RedirectProbeResult {
        private final boolean available;
        private final boolean redirectObserved;
        private final boolean loopDetected;
        private final boolean maxHopsExceeded;
        private final boolean privateTargetBlocked;
        private final boolean failed;
        private final int domainChanges;
        private final URI finalUri;
        private final List<URI> chain;
        private final String message;

        private RedirectProbeResult(boolean available,
                                    boolean redirectObserved,
                                    boolean loopDetected,
                                    boolean maxHopsExceeded,
                                    boolean privateTargetBlocked,
                                    boolean failed,
                                    int domainChanges,
                                    URI finalUri,
                                    List<URI> chain,
                                    String message) {
            this.available = available;
            this.redirectObserved = redirectObserved;
            this.loopDetected = loopDetected;
            this.maxHopsExceeded = maxHopsExceeded;
            this.privateTargetBlocked = privateTargetBlocked;
            this.failed = failed;
            this.domainChanges = domainChanges;
            this.finalUri = finalUri;
            this.chain = List.copyOf(chain);
            this.message = message;
        }

        private static RedirectProbeResult unavailable(String message) {
            return new RedirectProbeResult(false, false, false, false, false, false,
                0, null, List.of(), message);
        }

        private static RedirectProbeResult completed(List<URI> chain, URI finalUri,
                                                     boolean redirectObserved, int domainChanges) {
            return new RedirectProbeResult(true, redirectObserved, false, false, false, false,
                domainChanges, finalUri, chain, "");
        }

        private static RedirectProbeResult loop(List<URI> chain, URI finalUri, int domainChanges) {
            return new RedirectProbeResult(true, true, true, false, false, false,
                domainChanges, finalUri, chain, "Redirect loop detected");
        }

        private static RedirectProbeResult maxHops(List<URI> chain, URI finalUri, int domainChanges) {
            return new RedirectProbeResult(true, true, false, true, false, false,
                domainChanges, finalUri, chain, "Redirect chain exceeded hop limit");
        }

        private static RedirectProbeResult blocked(List<URI> chain, URI finalUri,
                                                   int domainChanges, String message) {
            return new RedirectProbeResult(true, true, false, false, true, false,
                domainChanges, finalUri, chain, message);
        }

        private static RedirectProbeResult failed(List<URI> chain, URI finalUri,
                                                  boolean redirectObserved,
                                                  int domainChanges, String message) {
            return new RedirectProbeResult(true, redirectObserved, false, false, false, true,
                domainChanges, finalUri, chain, message);
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isRedirectObserved() {
            return redirectObserved;
        }

        public boolean isLoopDetected() {
            return loopDetected;
        }

        public boolean isMaxHopsExceeded() {
            return maxHopsExceeded;
        }

        public boolean isPrivateTargetBlocked() {
            return privateTargetBlocked;
        }

        public boolean isFailed() {
            return failed;
        }

        public int getDomainChanges() {
            return domainChanges;
        }

        public URI getFinalUri() {
            return finalUri;
        }

        public List<URI> getChain() {
            return chain;
        }

        public String getMessage() {
            return message;
        }

        public int getRedirectHopCount() {
            return Math.max(0, chain.size() - 1);
        }
    }
}