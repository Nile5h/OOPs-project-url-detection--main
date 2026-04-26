package com.url_detector.desktop.ui;

import com.url_detector.desktop.model.HistoryRecord;
import com.url_detector.desktop.service.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * History section with scan records table and management controls.
 */
public class HistorySection {
    private VBox node;
    private TableView<HistoryRecord> historyTable;
    private ComboBox<String> riskFilter;
    private Label recordCountLabel;

    public HistorySection() {
        buildUI();
        loadHistory("all");
    }

    private void buildUI() {
        node = new VBox();
        node.setStyle("-fx-background-color: var(--bg); -fx-padding: 60 40;");
        node.setSpacing(30);

        // Section header
        Label sectionTitle = new Label("History");
        sectionTitle.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: var(--ink);");

        Label sectionSubtitle = new Label("Review previously scanned URLs and their risk assessments.");
        sectionSubtitle.setStyle("-fx-font-size: 14; -fx-text-fill: var(--muted);");

        VBox header = new VBox();
        header.setSpacing(5);
        header.getChildren().addAll(sectionTitle, sectionSubtitle);

        // Controls
        HBox controls = new HBox();
        controls.setSpacing(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Filter by Risk:");
        filterLabel.setStyle("-fx-font-size: 12; -fx-text-fill: var(--ink);");

        riskFilter = new ComboBox<>();
        riskFilter.setItems(FXCollections.observableArrayList(
                "all", "SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL"
        ));
        riskFilter.setValue("all");
        riskFilter.setStyle(
                "-fx-font-size: 12;" +
                "-fx-padding: 6 12;" +
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);"
        );
        riskFilter.setOnAction(e -> loadHistory(riskFilter.getValue().toLowerCase()));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle(
                "-fx-padding: 6 12;" +
                "-fx-background-color: var(--surface-2);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;"
        );
        refreshBtn.setOnAction(e -> loadHistory(riskFilter.getValue().toLowerCase()));

        recordCountLabel = new Label("Loading...");
        recordCountLabel.setStyle("-fx-font-size: 12; -fx-text-fill: var(--muted);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = new Button("Clear History");
        clearBtn.setStyle(
                "-fx-padding: 6 12;" +
                "-fx-background-color: var(--critical);" +
                "-fx-text-fill: white;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;"
        );
        clearBtn.setOnAction(e -> clearHistory());

        controls.getChildren().addAll(filterLabel, riskFilter, refreshBtn, recordCountLabel, spacer, clearBtn);

        // Table
        historyTable = new TableView<>();
        historyTable.setStyle(
                "-fx-font-size: 12;" +
                "-fx-control-inner-background: var(--surface);" +
                "-fx-text-fill: var(--ink);"
        );
        historyTable.setPrefHeight(300);

        TableColumn<HistoryRecord, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlCol.setPrefWidth(300);

        TableColumn<HistoryRecord, String> riskCol = new TableColumn<>("Risk");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("risk"));
        riskCol.setPrefWidth(80);

        TableColumn<HistoryRecord, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(60);

        TableColumn<HistoryRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            HistoryRecord record = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(record.getFormattedTime());
        });
        timeCol.setPrefWidth(80);

        historyTable.getColumns().addAll(urlCol, riskCol, scoreCol, timeCol);

        node.getChildren().addAll(header, controls, historyTable);
    }

    private void loadHistory(String riskFilter) {
        new Thread(() -> {
            try {
                java.util.List<HistoryRecord> records = ApiClient.getHistory(riskFilter);
                Platform.runLater(() -> {
                    ObservableList<HistoryRecord> data = FXCollections.observableArrayList(records);
                    historyTable.setItems(data);
                    recordCountLabel.setText(records.size() + " record" + (records.size() != 1 ? "s" : ""));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    recordCountLabel.setText("Error loading history");
                    historyTable.setItems(FXCollections.observableArrayList());
                });
            }
        }).start();
    }

    private void clearHistory() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Clear History");
        confirmation.setHeaderText("Delete all scan records?");
        confirmation.setContentText("This action cannot be undone.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            new Thread(() -> {
                try {
                    boolean success = ApiClient.clearHistory();
                    Platform.runLater(() -> {
                        if (success) {
                            Alert success_alert = new Alert(Alert.AlertType.INFORMATION);
                            success_alert.setTitle("Success");
                            success_alert.setHeaderText("History cleared");
                            success_alert.showAndWait();
                            loadHistory(riskFilter.getValue().toLowerCase());
                        } else {
                            Alert error_alert = new Alert(Alert.AlertType.ERROR);
                            error_alert.setTitle("Error");
                            error_alert.setHeaderText("Failed to clear history");
                            error_alert.showAndWait();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert error_alert = new Alert(Alert.AlertType.ERROR);
                        error_alert.setTitle("Error");
                        error_alert.setHeaderText("Failed to clear history: " + e.getMessage());
                        error_alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    public VBox getNode() {
        return node;
    }
}
