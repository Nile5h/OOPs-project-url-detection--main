package com.url_detector.desktop.model;

import java.util.ArrayList;
import java.util.List;

public class HistoryResponse {
    private List<HistoryRecord> records = new ArrayList<>();

    public List<HistoryRecord> getRecords() {
        return records;
    }

    public void setRecords(List<HistoryRecord> records) {
        this.records = records == null ? new ArrayList<>() : records;
    }
}
