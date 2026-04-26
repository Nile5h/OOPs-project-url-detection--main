package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Inspects the domain and TLD for known suspicious patterns:
 * risky TLDs, excessive subdomains, and brand names used inside subdomains
 * to impersonate legitimate services.
 */
public class DomainChecker implements UrlChecker {

    // TLDs commonly abused for phishing (free / unregulated)
    private static final Set<String> SUSPICIOUS_TLDS = Set.of(
        "tk", "ml", "ga", "cf", "gq", "xyz", "top", "pw", "cc", "ws",
        "click", "link", "online", "site", "info", "biz", "vip", "live"
    );

    // Well-known brand names that should only appear as the registrable domain,
    // never as a subdomain of a different domain.
    private static final Set<String> BRAND_NAMES = Set.of(
        "paypal", "amazon", "google", "facebook", "apple", "microsoft",
        "netflix", "ebay", "instagram", "twitter", "linkedin", "youtube",
        "bankofamerica", "chase", "citibank", "wellsfargo", "usbank",
        "steam", "discord", "roblox", "fortnite", "tiktok", "whatsapp",
        "dropbox", "spotify", "adobe", "zoom", "stripe", "icloud"
    );

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();

        // ── Suspicious TLD ───────────────────────────────────────────────────
        String tld = url.getTld().toLowerCase();
        if (!tld.isBlank() && SUSPICIOUS_TLDS.contains(tld)) {
            results.add(new CheckResult(20, "URL uses a high-risk TLD: ." + tld));
        }

        // ── Too many subdomains ───────────────────────────────────────────────
        List<String> subs = url.getSubdomains();
        if (subs.size() > 2) {
            results.add(new CheckResult(15, "Unusual number of subdomains: " + subs.size()));
        }

        // ── Brand name in subdomain (impersonation) ───────────────────────────
        for (String sub : subs) {
            String lowerSub = sub.toLowerCase();
            for (String brand : BRAND_NAMES) {
                if (lowerSub.contains(brand)) {
                    results.add(new CheckResult(35,
                        "Brand name '" + brand + "' appears as a subdomain — possible impersonation"));
                    break; // one flag per subdomain label is enough
                }
            }
        }

        return results;
    }
}
