# Suspicious URL Detector

A Java URL threat-intelligence platform with two frontends:
- Web frontend (served by the embedded HTTP server)
- Desktop frontend (JavaFX)

It analyzes URLs for phishing, malware, and scam indicators using an ensemble of blacklist checks, CSV intelligence feeds, structure/keyword/domain heuristics, typosquat detection, redirect probing, and Bayesian feature scoring.

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
- Desktop JavaFX application with matching visual theme and native dialogs
- Unified launcher in `Main` for web or desktop mode
- Frontend assets are fully local (no external CSS/font CDN dependency)
- Eleven independent checkers that each contribute to the final risk score
- SQLite-backed scan history with filtering by risk level
- API endpoints: health, analyze, history (GET + DELETE)
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

### Web mode (default)

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar
```

Open `http://localhost:8080` in your browser.

Custom port:

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar 8091
```

Explicit web mode:

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar --web 8091
```

### Desktop mode (JavaFX)

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar --desktop
```

Or via JavaFX plugin during development:

```bash
mvn javafx:run
```

### Help

```bash
java -jar target/url_detector-1.0-SNAPSHOT.jar --help
```

The application creates `url_detector.db` in the working directory on first launch to store scan history.

---

## Project Structure

```
.
├── pom.xml
└── src/main/
    ├── java/com/url_detector/
    │   ├── Main.java                        ← Unified launcher (web/desktop)
    │   ├── desktop/
    │   │   ├── DesktopApp.java              ← JavaFX desktop UI
    │   │   ├── DesktopLauncher.java         ← Desktop-only launcher helper
    │   │   ├── model/
    │   │   │   ├── AnalyzeResponse.java
    │   │   │   ├── HealthResponse.java
    │   │   │   ├── HistoryRecord.java
    │   │   │   └── HistoryResponse.java
    │   │   └── service/
    │   │       └── ApiClient.java           ← HTTP client for backend endpoints
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
    │   │   ├── UrlDatasetChecker.java       ← Allowlist/risk reduction from url_dataset.csv
    │   │   └── UrisScamChecker.java         ← Scam/phishing feed from uris.csv
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
        ├── desktop-style.css                ← JavaFX desktop stylesheet
        └── static/
            ├── index.html                   ← Web dashboard UI
            └── assets/
                ├── styles.css               ← Dashboard styling
                └── app.js                   ← Frontend behavior and API calls
        ├── blacklist.txt                    ← Known malicious domains
        ├── PhishingData.csv                 ← Feature dataset used by PhishingDataChecker
        ├── PhisingData.csv                  ← Additional phishing feature dataset
        ├── popular_domains.txt              ← Top domains for typosquat comparison
        ├── uris.csv                         ← Scam/phishing intelligence feed
        └── url_dataset.csv                  ← Legitimate URL dataset used for risk reduction
```

---

## Java Files — What Each One Does

### Entry Point

**`Main.java`**
The unified application entry point.
- Default: starts web mode on port 8080
- `--web [port]`: explicit web mode
- `--desktop`: starts JavaFX desktop mode
- `--help`: prints launcher usage

---

### `desktop/`

**`DesktopApp.java`**
JavaFX desktop client that mirrors the web frontend sections (Overview, Analyze, History, Features), polls health status, supports dark/light themes, and calls existing backend APIs.

**`DesktopLauncher.java`**
Desktop launch helper class for JavaFX-specific startup wiring.

**`desktop/service/ApiClient.java`**
HTTP client used by desktop UI to call:
- `/api/health`
- `/api/analyze`
- `/api/history` (GET)
- `/api/history` (DELETE)

**`desktop/model/*`**
DTO models for mapping JSON API responses in desktop mode.

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
Loads `PhishingData.csv` and `PhisingData.csv`, then trains a lightweight Naive-Bayes model over URL-derivable features (IP host, URL length band, shortener usage, `@`, double-slash redirect pattern, hyphenated host, subdomain depth, port anomalies, and `https` token in host). Adds or subtracts score based on predicted phishing probability.

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

**`UrlDatasetChecker.java`**
Loads `url_dataset.csv` as a legitimate-domain reference set and applies negative scoring on host/domain matches to reduce false positives.

**`UrisScamChecker.java`**
Loads `uris.csv` (scam/phishing threat feed) and applies high-confidence positive scores for exact URL or host matches.

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
- supports `DELETE /api/history` to clear scan history
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
| `PhishingData.csv` | Feature dataset used by `PhishingDataChecker` for Bayesian phishing-likelihood scoring. |
| `PhisingData.csv` | Additional phishing feature dataset loaded by `PhishingDataChecker`. |
| `popular_domains.txt` | One domain per line. Used by `TyposquatChecker` for edit-distance comparison. |
| `url_dataset.csv` | Legitimate URL/domain set used for risk reduction in `UrlDatasetChecker`. |
| `uris.csv` | Scam/phishing threat-intelligence feed used by `UrisScamChecker`. |
| `desktop-style.css` | JavaFX stylesheet for the desktop application. |
| `malicious_phish.csv` | Optional legacy malicious feed used by `CsvMaliciousListChecker` when bundled at runtime. |
| `safe_sites_df.csv` | Optional legacy safe-domain feed used by `CsvSafeListChecker` when bundled at runtime. |

Files under `src/main/resources` are bundled inside the JAR under the classpath root and loaded via `getClass().getResourceAsStream(...)`.
`PhishingData.csv` is also supported from the project root as a runtime fallback.

---

## Dependencies

Defined in `pom.xml`:

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.xerial:sqlite-jdbc` | 3.46.0.0 | SQLite JDBC driver — enables the embedded database for scan history without requiring an external database server |
| `org.slf4j:slf4j-simple` | 1.7.36 | Simple SLF4J logging backend — required by the SQLite JDBC driver to suppress "no SLF4J provider" warnings at runtime |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.2 | JSON mapping for desktop API client responses |
| `org.openjfx:javafx-controls` | 17.0.11 | JavaFX desktop UI controls |
| `org.openjfx:javafx-graphics` | 17.0.11 | JavaFX rendering/runtime |
| `org.openjfx:javafx-base` | 17.0.11 | JavaFX base classes |

### Build Plugins

| Plugin | Version | Purpose |
|--------|---------|---------|
| `maven-jar-plugin` | 3.3.0 | Sets `com.url_detector.Main` as the JAR manifest main class |
| `maven-shade-plugin` | 3.5.0 | Packages all dependencies into a single fat JAR so the application can be run with `java -jar` without a separate classpath |
| `javafx-maven-plugin` | 0.0.8 | Allows local desktop execution with `mvn javafx:run` |

The project uses Java standard library components plus the dependencies listed above. HTTP client calls in desktop mode use Java's built-in `java.net.http` package.

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
