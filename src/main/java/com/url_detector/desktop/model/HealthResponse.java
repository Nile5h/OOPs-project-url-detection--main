package com.url_detector.desktop.model;

/**
 * Response model for the health check API endpoint.
 */
public class HealthResponse {
    public String status;
    public long timestamp;

    public HealthResponse() {
    }

    public HealthResponse(String status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public boolean isHealthy() {
        return "ok".equalsIgnoreCase(status);
    }
}
