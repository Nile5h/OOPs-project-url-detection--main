package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;
import com.url_detector.util.RedirectProbeUtil;
import com.url_detector.util.UrlNormalizationUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs live redirect-chain checks to detect suspicious bounce-back behavior.
 */
public class HttpRedirectChecker implements UrlChecker {

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        RedirectProbeUtil.RedirectProbeResult probe = RedirectProbeUtil.probe(url.getRawUrl());
        if (!probe.isAvailable()) {
            return results;
        }

        if (!probe.isRedirectObserved()) {
            return results;
        }

        int hops = probe.getRedirectHopCount();
        if (hops >= 3) {
            results.add(new CheckResult(20,
                "Long redirect chain observed (" + hops + " hops)"));
        }

        int domainChanges = probe.getDomainChanges();
        if (domainChanges > 0) {
            results.add(new CheckResult(25,
                "Redirect chain changes domain " + domainChanges + " time(s)"));
        }

        String initialHost = UrlNormalizationUtil.normalizeHost(url.getHost());
        URI finalUri = probe.getFinalUri();
        if (finalUri != null) {
            String finalHost = UrlNormalizationUtil.normalizeHost(finalUri.getHost());
            String initialRegistrable = registrableDomain(initialHost);
            String finalRegistrable = registrableDomain(finalHost);
            if (!initialRegistrable.isBlank() && !finalRegistrable.isBlank()
                && !initialRegistrable.equals(finalRegistrable)) {
                results.add(new CheckResult(10,
                    "Final redirect target differs from original host: " + finalHost));
            }
        }

        if (probe.isLoopDetected()) {
            results.add(new CheckResult(35, "Redirect loop/bounce-back detected"));
        }

        if (probe.isMaxHopsExceeded()) {
            results.add(new CheckResult(20, "Redirect chain exceeded safe hop limit"));
        }

        if (probe.isPrivateTargetBlocked()) {
            results.add(new CheckResult(30,
                "Redirect points to local/private network target (blocked)"));
        }

        if (probe.isFailed()) {
            results.add(new CheckResult(5,
                "Redirect chain could not be fully resolved: " + probe.getMessage()));
        }

        return results;
    }

    private String registrableDomain(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String[] labels = host.split("\\.");
        if (labels.length < 2) {
            return host;
        }
        return labels[labels.length - 2] + "." + labels[labels.length - 1];
    }
}