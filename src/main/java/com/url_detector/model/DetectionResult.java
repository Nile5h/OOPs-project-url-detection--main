package com.url_detector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The complete result of analyzing a single URL.
 */
public class DetectionResult {

    private final ParsedUrl parsedUrl;
    private final int totalScore;
    private final RiskLevel riskLevel;
    private final List<String> flags; // human-readable reasons

    public DetectionResult(ParsedUrl parsedUrl, int totalScore, List<String> flags) {
        this.parsedUrl = parsedUrl;
        this.totalScore = totalScore;
        this.riskLevel = RiskLevel.fromScore(totalScore);
        this.flags = Collections.unmodifiableList(new ArrayList<>(flags));
    }

    public ParsedUrl getParsedUrl()  { return parsedUrl; }
    public int getTotalScore()       { return totalScore; }
    public RiskLevel getRiskLevel()  { return riskLevel; }
    public List<String> getFlags()   { return flags; }

    /**
     * Prints a formatted single-URL report to stdout.
     */
    public void printReport() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  URL        : %-47s║%n", truncate(parsedUrl.getRawUrl(), 47));
        System.out.printf ("║  Risk Level : %-47s║%n", riskLevelColored());
        System.out.printf ("║  Score      : %-47s║%n", totalScore);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        if (flags.isEmpty()) {
            System.out.println("║  No suspicious signals detected.                             ║");
        } else {
            System.out.println("║  Detected Signals:                                           ║");
            for (String flag : flags) {
                System.out.printf("║   • %-58s║%n", truncate(flag, 58));
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    private String riskLevelColored() {
        return switch (riskLevel) {
            case SAFE     -> "[ SAFE     ]  Score: " + totalScore;
            case LOW      -> "[ LOW      ]  Score: " + totalScore;
            case MEDIUM   -> "[ MEDIUM   ]  Score: " + totalScore;
            case HIGH     -> "[ HIGH     ]  Score: " + totalScore;
            case CRITICAL -> "[ CRITICAL ]  Score: " + totalScore;
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
