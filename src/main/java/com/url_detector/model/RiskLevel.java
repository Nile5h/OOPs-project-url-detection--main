package com.url_detector.model;

public enum RiskLevel {
    SAFE("SAFE", 0),
    LOW("LOW", 16),
    MEDIUM("MEDIUM", 31),
    HIGH("HIGH", 56),
    CRITICAL("CRITICAL", 81);

    private final String label;
    private final int minScore;

    RiskLevel(String label, int minScore) {
        this.label = label;
        this.minScore = minScore;
    }

    public String getLabel() {
        return label;
    }

    public int getMinScore() {
        return minScore;
    }

    /**
     * Maps a numeric score to the appropriate RiskLevel.
     */
    public static RiskLevel fromScore(int score) {
        RiskLevel result = SAFE;
        for (RiskLevel level : values()) {
            if (score >= level.minScore) {
                result = level;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return label;
    }
}
