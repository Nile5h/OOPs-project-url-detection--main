# Suspicious URL Detector

A Java web application that analyzes URLs for phishing, malware, and other suspicious indicators. It combines multiple detection strategies — blacklists, CSV threat-intelligence datasets, structural analysis, keyword scanning, domain checks, and typosquatting detection — to produce a risk score and human-readable report for each URL.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [How to Build](#how-to-build)
- [How to Run](#how-to-run)
- [Project Structure](#project-structure)
- [Java Files — What Each One Does](#java-files--what-each-one-does)
- [Resource Files](#resource-files)
- [Dependencies](#dependencies)
- [Risk Scoring System](#risk-scoring-system)
- [Database](#database)

---

## Features

- Professional web dashboard with URL analysis and live scan history
- Frontend assets are fully local (no external CSS/font CDN dependency)
- Nine independent checkers that each contribute a score
- SQLite-backed scan history with filtering by risk level
- Live redirect-chain checks with hop limits, loop detection, and private-target blocking
- Fat JAR output (all dependencies bundled)

---

## Requirements

| Tool | Version |
|------|---------|
| JDK  | 17 or later |
| Maven | 3.8 or later |

Make sure `JAVA_HOME` points to your JDK root (e.g. `C:\Program Files\Java\jdk-21`) and `%JAVA_HOME%\bin` is on your `PATH`.

---

## How to Build

```bash
mvn package
```

This compiles the code, runs tests, and produces a fat JAR at:

`target/url_detector-1.0-SNAPSHOT.jar`

---

## How to Run

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar
```

Open `http://localhost:8080` in your browser. You can also pass a custom port, for example:

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar 8091
```

The application creates `url_detector.db` in the working directory on first launch to store scan history.

---

## Project Structure

```
.
├── pom.xml
└── src/main/
    ├── java/com/url_detector/
    │   ├── Main.java                        ← Entry point
    │   ├── analyzer/
    │   │   └── UrlAnalyzer.java             ← Orchestrates all checkers
    │   ├── app/
    │   │   └── UrlDetectorApp.java          ← Legacy CLI menu (not used by GUI)
    │   ├── checker/
    │   │   ├── UrlChecker.java              ← Interface all checkers implement
    │   │   ├── CheckResult.java             ← Single checker output (score + reason)
    │   │   ├── BlacklistChecker.java        ← Checks against blacklist.txt
    │   │   ├── CsvMaliciousListChecker.java ← Checks against malicious_phish.csv
    │   │   ├── CsvSafeListChecker.java      ← Allowlist from safe_sites_df.csv
    │   │   ├── DomainChecker.java           ← TLD, subdomain, brand impersonation
    │   │   ├── HttpRedirectChecker.java     ← Live HTTP redirect-chain analysis
    │   │   ├── KeywordChecker.java          ← Phishing keyword scan
    │   │   ├── PhishingDataChecker.java     ← Naive-Bayes signal from PhishingData.csv
    │   │   ├── StructureChecker.java        ← URL structure anomalies
    │   │   └── TyposquatChecker.java        ← Levenshtein distance vs popular domains
    │   ├── db/
    │   │   ├── DatabaseManager.java         ← SQLite connection + schema creation
    │   │   ├── ScanHistoryRepository.java   ← CRUD for scan_history table
    │   │   └── ScanRecord.java              ← Data class for one history row
    │   ├── model/
    │   │   ├── ParsedUrl.java               ← Immutable URL breakdown
    │   │   ├── DetectionResult.java         ← Final result: score + risk + flags
    │   │   └── RiskLevel.java               ← Enum: SAFE / LOW / MEDIUM / HIGH / CRITICAL
    │   ├── parser/
    │   │   └── UrlParser.java               ← Parses raw string into ParsedUrl
    │   ├── service/
    │   │   └── UrlDetectionService.java     ← Core service used by API handlers
    │   └── util/
    │       ├── LevenshteinUtil.java         ← Edit-distance algorithm
    │       ├── RedirectProbeUtil.java       ← Safe bounded redirect probe utility
    │       └── UrlNormalizationUtil.java    ← URL/host key normalization
    └── resources/
        └── static/
            ├── index.html                   ← Web dashboard UI
            └── assets/
                ├── styles.css               ← Dashboard styling
                └── app.js                   ← Frontend behavior and API calls
        ├── blacklist.txt                    ← Known malicious domains
        ├── malicious_phish.csv              ← Phishing/malware URL dataset
        ├── popular_domains.txt              ← Top domains for typosquat comparison
        └── safe_sites_df.csv                ← Allowlisted safe domains
    └── PhishingData.csv                         ← Feature dataset used by PhishingDataChecker
```

---

## Java Files — What Each One Does

### Entry Point

**`Main.java`**
The application entry point. Starts the embedded HTTP server and serves the web dashboard and REST API.

---

### `analyzer/`

**`UrlAnalyzer.java`**
The core orchestrator. Holds a fixed list of all `UrlChecker` implementations and runs each one against a `ParsedUrl`. Accumulates scores and flag messages, floors the total at 0 (so allowlist deductions never produce a negative score), and returns a `DetectionResult`.

---

### `app/`

**`UrlDetectorApp.java`**
Legacy launcher retained for compatibility. Delegates to `Main`.

---

### `checker/`

**`UrlChecker.java`** *(interface)*
The contract every checker must implement. Defines one method: `List<CheckResult> check(ParsedUrl url)`. Returns an empty list if nothing suspicious is found.

**`CheckResult.java`**
A simple value object holding a numeric `score` and a human-readable `reason` string. A result with a non-zero score and non-blank reason is considered a flag (`hasFlag()` returns true). Negative scores are valid — they represent allowlist deductions.

**`BlacklistChecker.java`**
Loads `blacklist.txt` from the classpath into a `HashSet` at startup. On each check, looks up the URL's registrable domain. A match scores **+100** (instant CRITICAL).

**`CsvMaliciousListChecker.java`**
Loads `malicious_phish.csv` at startup, skipping rows labelled `benign`. Builds two sets: one for exact normalized URL keys, one for host-only phishing/malware domains. An exact URL match scores **+120**; a domain-only match scores **+90**.

**`CsvSafeListChecker.java`**
Loads `safe_sites_df.csv` as an allowlist. If the URL's host or registrable domain is found, applies a **negative score** (−25 for exact host, −15 for registrable domain) to reduce false positives on legitimate sites.

**`DomainChecker.java`**
Checks three things:
- TLD is in a hardcoded set of high-risk TLDs (`.tk`, `.xyz`, `.top`, etc.) → **+20**
- More than 2 subdomains → **+15**
- A known brand name (PayPal, Amazon, Google, etc.) appears as a subdomain label → **+35** per brand found

**`HttpRedirectChecker.java`**
Performs live redirect-chain analysis using a bounded probe. Flags suspicious redirect behavior such as cross-domain hops, long redirect chains, loops/bounce-back patterns, and redirects to local/private targets.

**`KeywordChecker.java`**
Scans the full raw URL string for ~40 phishing-related keywords (`login`, `verify`, `password`, `bitcoin`, `suspended`, etc.). Scores **+10 per keyword**, capped at **+30** total.

**`PhishingDataChecker.java`**
Loads `PhishingData.csv` and trains a lightweight Naive-Bayes model over URL-derivable features (IP host, URL length band, shortener usage, `@`, double-slash redirect pattern, hyphenated host, subdomain depth, port anomalies, and `https` token in host). Adds or subtracts score based on predicted phishing probability.

**`StructureChecker.java`**
Detects structural anomalies:
- URL length > 100 chars → **+20**, length > 75 → **+10**
- IP address as host → **+30**
- Non-standard port present → **+10**
- `@` symbol in URL (credential injection) → **+25**
- Percent-encoded characters → **+15**
- More than 2 hyphens in domain → **+10**
- Double slashes `//` in path → **+10**
- Open redirect query parameters (`?url=`, `?redirect=`, etc.) → **+20**

**`TyposquatChecker.java`**
Loads `popular_domains.txt` and computes the Levenshtein edit distance between the URL's registrable domain and every popular domain. Distance of 1 → **+40** (stops checking). Distance of 2 → **+20** (continues in case a distance-1 match follows).

---

### `db/`

**`DatabaseManager.java`**
Manages the SQLite connection via JDBC. Takes a file path, builds the JDBC URL, and exposes `getConnection()`. Also runs `initializeSchema()` which creates the `scan_history` table if it does not exist.

**`ScanHistoryRepository.java`**
JDBC repository for the `scan_history` table. Provides:
- `save(DetectionResult)` — inserts a new row with timestamp
- `findRecent(int limit)` — returns the most recent N rows ordered by ID descending
- `searchByRisk(String riskLevel, int limit)` — filters by risk level
- `deleteAll()` — clears the entire table

**`ScanRecord.java`**
Immutable data class representing one row from `scan_history`: id, url, score, riskLevel, flagsText, scannedAt.

---

### `model/`

**`ParsedUrl.java`**
Immutable value object produced by `UrlParser`. Fields: `rawUrl`, `scheme`, `host`, `registrableDomain`, `tld`, `subdomains` (list), `path`, `query`, `port`, `ipBased`.

**`DetectionResult.java`**
The final output of analyzing one URL. Holds the `ParsedUrl`, `totalScore`, `RiskLevel` (derived from score), and an unmodifiable list of flag strings. Also contains `printReport()` which renders a formatted box-drawing report to stdout.

**`RiskLevel.java`**
Enum with five levels and their minimum score thresholds:

| Level    | Min Score |
|----------|-----------|
| SAFE     | 0         |
| LOW      | 16        |
| MEDIUM   | 31        |
| HIGH     | 56        |
| CRITICAL | 81        |

`fromScore(int)` iterates all levels and returns the highest one whose threshold is met.

---

### `parser/`

**`UrlParser.java`**
Converts a raw URL string into a `ParsedUrl`. Handles:
- Blank/null input → throws `IllegalArgumentException`
- UTF-8 BOM stripping
- Missing scheme → prepends `http://` for `java.net.URI` parsing
- IPv4 detection via regex
- Splitting host labels into TLD, registrable domain, and subdomains list

---

### `service/`

**`UrlDetectionService.java`**
A thin facade used by API handlers. Wraps `UrlParser` + `UrlAnalyzer` and exposes:
- `analyzeOne(String rawUrl)` — parse + analyze a single URL
- `analyzeBatch(List<String>)` — analyze a list, silently skipping malformed entries
- `analyzeFromFile(Path)` — reads a `.txt` file (skips blank lines and `#` comments), then calls `analyzeBatch`
- `summarize(List<DetectionResult>)` — returns a `Map<RiskLevel, Long>` count

---

### `web/`

**`WebServer.java`**
Embedded HTTP server that:
- serves static files from `src/main/resources/static`
- exposes `/api/health`, `/api/analyze`, and `/api/history`
- saves scans to SQLite via `ScanHistoryRepository`

---

### `util/`

**`LevenshteinUtil.java`**
Utility class (no instances) that computes the Levenshtein edit distance between two strings using a space-optimized two-row rolling array. Time complexity O(m×n), space O(n). Used exclusively by `TyposquatChecker`.

**`RedirectProbeUtil.java`**
Utility class (no instances) used by `HttpRedirectChecker` to perform safe redirect probing with manual redirect handling, hop limit, timeout budget, loop detection, and local/private-target blocking.

**`UrlNormalizationUtil.java`**
Utility class (no instances) with three static helpers:
- `normalizeHost(String)` — lowercases and strips leading `www.`
- `extractHost(String)` — extracts and normalizes the host from any raw URL string, with a fallback for malformed inputs
- `normalizeUrlKey(String)` — produces a canonical `scheme://host/path?query` key (no trailing slash, lowercased) used for exact-match lookups in the CSV checkers

---

## Resource Files

| File | Purpose |
|------|---------|
| `blacklist.txt` | One domain per line. Domains matched here score +100 instantly. Lines starting with `#` are comments. |
| `malicious_phish.csv` | Two-column CSV (`url,type`). Rows with type `benign` are ignored. Phishing/malware rows populate the malicious URL and host sets. |
| `popular_domains.txt` | One domain per line. Used by `TyposquatChecker` for edit-distance comparison. |
| `safe_sites_df.csv` | Two-column CSV (`domain,category`). Matched domains receive a negative score adjustment to reduce false positives. |
| `PhishingData.csv` | UCI-style feature dataset used by `PhishingDataChecker` for Naive-Bayes phishing likelihood scoring. |

Files under `src/main/resources` are bundled inside the JAR under the classpath root and loaded via `getClass().getResourceAsStream(...)`.
`PhishingData.csv` is also supported from the project root as a runtime fallback.

---

## Dependencies

Defined in `pom.xml`:

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.xerial:sqlite-jdbc` | 3.46.0.0 | SQLite JDBC driver — enables the embedded database for scan history without requiring an external database server |
| `org.slf4j:slf4j-simple` | 1.7.36 | Simple SLF4J logging backend — required by the SQLite JDBC driver to suppress "no SLF4J provider" warnings at runtime |

### Build Plugins

| Plugin | Version | Purpose |
|--------|---------|---------|
| `maven-jar-plugin` | 3.3.0 | Sets `com.url_detector.Main` as the JAR manifest main class |
| `maven-shade-plugin` | 3.5.0 | Packages all dependencies into a single fat JAR so the application can be run with `java -jar` without a separate classpath |

The project uses only the Java standard library beyond these two runtime dependencies — no external HTTP clients, no ML frameworks, no third-party UI toolkits.

---

## Risk Scoring System

Each checker independently adds (or subtracts) points. All checker scores are summed, floored at 0, and mapped to a risk level:

```
Score 0–15    → SAFE
Score 16–30   → LOW
Score 31–55   → MEDIUM
Score 56–80   → HIGH
Score 81+     → CRITICAL
```

A URL can reach CRITICAL from a single blacklist or CSV dataset hit, or by accumulating points across multiple checkers. The allowlist checker (safe sites CSV) can reduce the total score to help avoid false positives on well-known legitimate domains.
