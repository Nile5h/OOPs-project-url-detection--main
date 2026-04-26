package com.url_detector.desktop.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for history records from the API.
 */
public class HistoryRecord {
    public int id;
    public String url;
    public String risk;
    public int score;
    public List<String> flags;
    public long timestamp;

    public HistoryRecord() {
        this.flags = new ArrayList<>();
    }

    public HistoryRecord(int id, String url, String risk, int score, List<String> flags, long timestamp) {
        this.id = id;
        this.url = url;
        this.risk = risk;
        this.score = score;
        this.flags = flags != null ? flags : new ArrayList<>();
        this.timestamp = timestamp;
    }

    /**
     * Get color for risk level in hex format.
     */
    public String getRiskColor() {
        return switch (risk.toLowerCase()) {
            case "critical" -> "#b4232a";
            case "high" -> "#d04b22";
            case "medium" -> "#bf7a00";
            case "low" -> "#2a8a37";
            case "safe" -> "#1f7a4f";
            default -> "#1259c3";
        };
    }

    /**
     * Format timestamp as human-readable date string.
     */
    public String getFormattedTime() {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }
}
