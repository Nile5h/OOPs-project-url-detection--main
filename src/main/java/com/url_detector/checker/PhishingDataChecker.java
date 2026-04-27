package com.url_detector.checker;

import com.url_detector.model.ParsedUrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Learns feature likelihoods from PhishingData.csv (UCI-style feature dataset)
 * and contributes a calibrated risk score from URL-derived features.
 */
public class PhishingDataChecker implements UrlChecker {

    private static final List<DatasetSource> DATASETS = List.of(
        new DatasetSource("/PhishingData.csv", "PhishingData.csv"),
        new DatasetSource("/PhisingData.csv", "PhisingData.csv")
    );

    private static final String COL_IP = "having_iphaving_ip_address";
    private static final String COL_URL_LENGTH = "urlurl_length";
    private static final String COL_SHORTENER = "shortining_service";
    private static final String COL_AT_SYMBOL = "having_at_symbol";
    private static final String COL_DOUBLE_SLASH = "double_slash_redirecting";
    private static final String COL_PREFIX_SUFFIX = "prefix_suffix";
    private static final String COL_SUBDOMAIN = "having_sub_domain";
    private static final String COL_PORT = "port";
    private static final String COL_HTTPS_TOKEN = "https_token";
    private static final String COL_RESULT = "result";

    private static final List<String> FEATURE_COLUMNS = List.of(
        COL_IP,
        COL_URL_LENGTH,
        COL_SHORTENER,
        COL_AT_SYMBOL,
        COL_DOUBLE_SLASH,
        COL_PREFIX_SUFFIX,
        COL_SUBDOMAIN,
        COL_PORT,
        COL_HTTPS_TOKEN
    );

    private static final Set<String> URL_SHORTENERS = Set.of(
        "bit.ly", "goo.gl", "tinyurl.com", "t.co", "ow.ly", "is.gd", "buff.ly",
        "rebrand.ly", "lnkd.in", "cutt.ly", "shorturl.at", "tiny.cc", "soo.gd"
    );

    private final Model model;

    public PhishingDataChecker() {
        this.model = trainModel();
    }

    @Override
    public List<CheckResult> check(ParsedUrl url) {
        List<CheckResult> results = new ArrayList<>();
        if (!model.isReady()) {
            return results;
        }

        Map<String, Integer> features = extractFeatures(url);
        double phishingProbability = model.predictPhishingProbability(features);

        int score;
        if (phishingProbability >= 0.90) {
            score = 45;
        } else if (phishingProbability >= 0.75) {
            score = 30;
        } else if (phishingProbability >= 0.60) {
            score = 20;
        } else if (phishingProbability <= 0.20) {
            score = -10;
        } else {
            score = 0;
        }

        if (score != 0) {
            String pct = String.format(Locale.ROOT, "%.1f", phishingProbability * 100.0);
            results.add(new CheckResult(score,
                "PhishingData model signal: " + pct + "% phishing likelihood"));
        }

        return results;
    }

    private Map<String, Integer> extractFeatures(ParsedUrl url) {
        Map<String, Integer> features = new HashMap<>();

        String raw = url.getRawUrl().toLowerCase(Locale.ROOT);
        String host = url.getHost().toLowerCase(Locale.ROOT);
        String registrable = url.getRegistrableDomain().toLowerCase(Locale.ROOT);

        features.put(COL_IP, url.isIpBased() ? -1 : 1);

        int length = url.getRawUrl().length();
        int lengthValue = length < 54 ? 1 : (length <= 75 ? 0 : -1);
        features.put(COL_URL_LENGTH, lengthValue);

        boolean shortener = URL_SHORTENERS.contains(host) || URL_SHORTENERS.contains(registrable);
        features.put(COL_SHORTENER, shortener ? -1 : 1);

        features.put(COL_AT_SYMBOL, raw.contains("@") ? -1 : 1);

        int schemePos = raw.indexOf("://");
        boolean doubleSlashRedirect = false;
        if (schemePos >= 0) {
            int secondSlash = raw.indexOf("//", schemePos + 3);
            doubleSlashRedirect = secondSlash >= 0;
        }
        features.put(COL_DOUBLE_SLASH, doubleSlashRedirect ? -1 : 1);

        features.put(COL_PREFIX_SUFFIX, host.contains("-") ? -1 : 1);

        int labels = host.isBlank() ? 0 : host.split("\\.").length;
        int subDomainValue = labels <= 2 ? 1 : (labels == 3 ? 0 : -1);
        features.put(COL_SUBDOMAIN, subDomainValue);

        boolean explicitNonDefaultPort = url.getPort() != -1
            && !(("http".equals(url.getScheme()) && url.getPort() == 80)
            || ("https".equals(url.getScheme()) && url.getPort() == 443));
        features.put(COL_PORT, explicitNonDefaultPort ? -1 : 1);

        features.put(COL_HTTPS_TOKEN, host.contains("https") ? -1 : 1);

        return features;
    }

    private Model trainModel() {
        Model model = new Model(FEATURE_COLUMNS);
        boolean loadedAny = false;

        for (DatasetSource source : DATASETS) {
            try (BufferedReader reader = openDatasetReader(source)) {
                if (reader == null) {
                    continue;
                }
                loadedAny = true;
                loadDataset(reader, model);
            } catch (IOException e) {
                System.err.println("[PhishingDataChecker] Warning: could not load "
                    + source.fallbackFile + ": " + e.getMessage());
            }
        }

        if (!loadedAny) {
            return Model.unavailable();
        }

        model.finish();
        return model.isReady() ? model : Model.unavailable();
    }

    private void loadDataset(BufferedReader reader, Model model) throws IOException {
        String header = reader.readLine();
        if (header == null) {
            return;
        }

        String[] rawHeaders = header.split(",");
        Map<String, Integer> indexByName = new HashMap<>();
        for (int i = 0; i < rawHeaders.length; i++) {
            indexByName.put(normalizeName(rawHeaders[i]), i);
        }

        for (String column : FEATURE_COLUMNS) {
            if (!indexByName.containsKey(column)) {
                return;
            }
        }
        if (!indexByName.containsKey(COL_RESULT)) {
            return;
        }

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] cols = splitCsvLine(line, rawHeaders.length);
            Integer result = parseInt(cols[indexByName.get(COL_RESULT)]);
            if (result == null) {
                continue;
            }

            boolean phishing = result == -1;
            Map<String, Integer> row = new HashMap<>();
            boolean valid = true;
            for (String feature : FEATURE_COLUMNS) {
                Integer value = parseInt(cols[indexByName.get(feature)]);
                if (value == null) {
                    valid = false;
                    break;
                }
                row.put(feature, value);
            }

            if (valid) {
                model.observe(row, phishing);
            }
        }
    }

    private BufferedReader openDatasetReader(DatasetSource source) throws IOException {
        InputStream is = getClass().getResourceAsStream(source.resourcePath);
        if (is != null) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        Path fallback = Path.of(source.fallbackFile);
        if (Files.exists(fallback)) {
            return Files.newBufferedReader(fallback, StandardCharsets.UTF_8);
        }

        return null;
    }

    private String normalizeName(String input) {
        return input == null
            ? ""
            : input.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String[] splitCsvLine(String line, int expectedColumns) {
        String[] parts = line.split(",", -1);
        if (parts.length >= expectedColumns) {
            return parts;
        }

        String[] resized = new String[expectedColumns];
        System.arraycopy(parts, 0, resized, 0, parts.length);
        for (int i = parts.length; i < expectedColumns; i++) {
            resized[i] = "";
        }
        return resized;
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class DatasetSource {
        private final String resourcePath;
        private final String fallbackFile;

        private DatasetSource(String resourcePath, String fallbackFile) {
            this.resourcePath = resourcePath;
            this.fallbackFile = fallbackFile;
        }
    }

    private static final class Model {
        private final List<String> features;
        private final Map<String, Map<Integer, Integer>> phishingCounts;
        private final Map<String, Map<Integer, Integer>> safeCounts;
        private int phishingRows;
        private int safeRows;
        private boolean ready;

        private Model(List<String> features) {
            this.features = features;
            this.phishingCounts = new HashMap<>();
            this.safeCounts = new HashMap<>();
            for (String feature : features) {
                phishingCounts.put(feature, new HashMap<>());
                safeCounts.put(feature, new HashMap<>());
            }
        }

        private static Model unavailable() {
            Model model = new Model(List.of());
            model.ready = false;
            return model;
        }

        private void observe(Map<String, Integer> row, boolean phishing) {
            if (phishing) {
                phishingRows++;
            } else {
                safeRows++;
            }

            for (String feature : features) {
                int value = row.get(feature);
                Map<Integer, Integer> bucket = phishing ? phishingCounts.get(feature) : safeCounts.get(feature);
                bucket.merge(value, 1, Integer::sum);
            }
        }

        private void finish() {
            ready = phishingRows > 0 && safeRows > 0 && !features.isEmpty();
        }

        private boolean isReady() {
            return ready;
        }

        private double predictPhishingProbability(Map<String, Integer> sample) {
            if (!ready) {
                return 0.0;
            }

            int totalRows = phishingRows + safeRows;
            double logPhishing = Math.log((double) phishingRows / totalRows);
            double logSafe = Math.log((double) safeRows / totalRows);

            for (String feature : features) {
                int value = sample.getOrDefault(feature, 0);
                int pCount = phishingCounts.get(feature).getOrDefault(value, 0);
                int sCount = safeCounts.get(feature).getOrDefault(value, 0);

                // Laplace smoothing with value-space size 3 (-1,0,1).
                double pLikelihood = (pCount + 1.0) / (phishingRows + 3.0);
                double sLikelihood = (sCount + 1.0) / (safeRows + 3.0);
                logPhishing += Math.log(pLikelihood);
                logSafe += Math.log(sLikelihood);
            }

            double max = Math.max(logPhishing, logSafe);
            double pExp = Math.exp(logPhishing - max);
            double sExp = Math.exp(logSafe - max);
            return pExp / (pExp + sExp);
        }
    }
}