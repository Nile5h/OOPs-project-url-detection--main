package com.url_detector.desktop.model;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeResponse {
    private String url;
    private int score;
    private String riskLevel;
    private List<String> flags = new ArrayList<>();

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

    public List<String> getFlags() {
        return flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags == null ? new ArrayList<>() : flags;
    }
}
