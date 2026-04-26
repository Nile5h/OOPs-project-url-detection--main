package com.url_detector.desktop.model;

public class HistoryRecord {
    private long id;
    private String url;
    private int score;
    private String riskLevel;
    private String flagsText;
    private String scannedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getFlagsText() {
        return flagsText;
    }

    public void setFlagsText(String flagsText) {
        this.flagsText = flagsText;
    }

    public String getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(String scannedAt) {
        this.scannedAt = scannedAt;
    }
}
