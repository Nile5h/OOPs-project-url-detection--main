package com.url_detector.db;

/**
 * Represents one persisted history row from the scan_history table.
 */
public class ScanRecord {

    private final long id;
    private final String url;
    private final int score;
    private final String riskLevel;
    private final String flagsText;
    private final String scannedAt;

    public ScanRecord(long id, String url, int score, String riskLevel, String flagsText, String scannedAt) {
        this.id = id;
        this.url = url;
        this.score = score;
        this.riskLevel = riskLevel;
        this.flagsText = flagsText;
        this.scannedAt = scannedAt;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getScore() {
        return score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getFlagsText() {
        return flagsText;
    }

    public String getScannedAt() {
        return scannedAt;
    }
}
