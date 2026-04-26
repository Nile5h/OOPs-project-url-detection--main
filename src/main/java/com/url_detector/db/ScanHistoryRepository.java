package com.url_detector.db;

import com.url_detector.model.DetectionResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for scan history persistence and querying.
 */
public class ScanHistoryRepository {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseManager databaseManager;

    public ScanHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(DetectionResult result) throws SQLException {
        String sql = "INSERT INTO scan_history(url, score, risk_level, flags_text, scanned_at) VALUES(?, ?, ?, ?, ?)";
        String flags = String.join(" | ", result.getFlags());

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, result.getParsedUrl().getRawUrl());
            statement.setInt(2, result.getTotalScore());
            statement.setString(3, result.getRiskLevel().name());
            statement.setString(4, flags);
            statement.setString(5, LocalDateTime.now().format(TS_FORMAT));
            statement.executeUpdate();
        }
    }

    public List<ScanRecord> findRecent(int limit) throws SQLException {
        String sql = "SELECT id, url, score, risk_level, flags_text, scanned_at FROM scan_history ORDER BY id DESC LIMIT ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRecords(rs);
            }
        }
    }

    public List<ScanRecord> searchByRisk(String riskLevel, int limit) throws SQLException {
        String sql = "SELECT id, url, score, risk_level, flags_text, scanned_at FROM scan_history WHERE risk_level = ? ORDER BY id DESC LIMIT ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, riskLevel);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRecords(rs);
            }
        }
    }

    public void deleteAll() throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM scan_history")) {
            statement.executeUpdate();
        }
    }

    private List<ScanRecord> mapRecords(ResultSet rs) throws SQLException {
        List<ScanRecord> records = new ArrayList<>();
        while (rs.next()) {
            records.add(new ScanRecord(
                rs.getLong("id"),
                rs.getString("url"),
                rs.getInt("score"),
                rs.getString("risk_level"),
                rs.getString("flags_text"),
                rs.getString("scanned_at")
            ));
        }
        return records;
    }
}
