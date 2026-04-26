package com.url_detector.desktop.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for the analyze URL API endpoint.
 */
public class AnalyzeResponse {
    public String url;
    public String risk;
    public int score;
    public List<String> flags;
    public long timestamp;

    public AnalyzeResponse() {
        this.flags = new ArrayList<>();
    }

    public AnalyzeResponse(String url, String risk, int score, List<String> flags, long timestamp) {
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
}
