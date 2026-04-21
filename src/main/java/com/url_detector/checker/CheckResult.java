package com.url_detector.checker;

/**
 * The result of a single checker's analysis of a URL.
 */
public class CheckResult {

    private final int score;
    private final String reason;

    public CheckResult(int score, String reason) {
        this.score = score;
        this.reason = reason;
    }

    /** A zero-score result with no finding. */
    public static CheckResult clean() {
        return new CheckResult(0, null);
    }

    public int getScore()    { return score; }
    public String getReason(){ return reason; }

    /**
     * Any non-zero score with a reason is treated as a meaningful signal.
     * This supports both positive risk boosts and negative allowlist reductions.
     */
    public boolean hasFlag() { return score != 0 && reason != null && !reason.isBlank(); }
}
