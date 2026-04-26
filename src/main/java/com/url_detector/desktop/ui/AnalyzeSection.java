package com.url_detector.desktop.ui;

import com.url_detector.desktop.model.AnalyzeResponse;
import com.url_detector.desktop.service.ApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Analyze section with URL input form and result display.
 */
public class AnalyzeSection {
    private VBox node;
    private TextArea urlInput;
    private Button scanButton;
    private VBox resultPanel;
    private Label errorText;

    public AnalyzeSection() {
        buildUI();
    }

    private void buildUI() {
        node = new VBox();
        node.setStyle("-fx-background-color: var(--bg); -fx-padding: 60 40;");
        node.setSpacing(30);

        // Section header
        Label sectionTitle = new Label("Analyze URL");
        sectionTitle.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: var(--ink);");

        Label sectionSubtitle = new Label("Paste a target URL and run a full risk evaluation.");
        sectionSubtitle.setStyle("-fx-font-size: 14; -fx-text-fill: var(--muted);");

        VBox header = new VBox();
        header.setSpacing(5);
        header.getChildren().addAll(sectionTitle, sectionSubtitle);

        // Form
        VBox form = new VBox();
        form.setStyle(
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 24;"
        );
        form.setSpacing(16);

        Label inputLabel = new Label("Target URL");
        inputLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: var(--ink);");

        urlInput = new TextArea();
        urlInput.setStyle(
                "-fx-font-size: 13;" +
                "-fx-text-fill: var(--ink);" +
                "-fx-control-inner-background: var(--surface-2);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 8;" +
                "-fx-font-family: 'Courier New';"
        );
        urlInput.setPrefHeight(60);
        urlInput.setWrapText(true);

        scanButton = new Button("Scan URL");
        scanButton.setStyle(
                "-fx-padding: 12 24;" +
                "-fx-background-color: var(--brand);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );

        scanButton.setOnAction(e -> performAnalysis());

        errorText = new Label();
        errorText.setStyle("-fx-text-fill: var(--critical); -fx-font-size: 12;");

        form.getChildren().addAll(inputLabel, urlInput, scanButton, errorText);

        // Result panel
        resultPanel = new VBox();
        resultPanel.setStyle(
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 24;"
        );
        resultPanel.setSpacing(16);
        resultPanel.setVisible(false);

        node.getChildren().addAll(header, form, resultPanel);
    }

    private void performAnalysis() {
        String url = urlInput.getText().trim();
        if (url.isEmpty()) {
            errorText.setText("Please enter a URL");
            return;
        }

        scanButton.setDisable(true);
        scanButton.setText("Scanning...");
        errorText.setText("");

        new Thread(() -> {
            try {
                AnalyzeResponse response = ApiClient.analyzeUrl(url);
                Platform.runLater(() -> displayResult(response));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorText.setText("Error: " + e.getMessage());
                    scanButton.setDisable(false);
                    scanButton.setText("Scan URL");
                });
            }
        }).start();
    }

    private void displayResult(AnalyzeResponse response) {
        resultPanel.getChildren().clear();

        // Risk badge
        VBox riskContainer = new VBox();
        riskContainer.setSpacing(8);

        Label riskLabel = new Label("Risk Level");
        riskLabel.setStyle("-fx-font-size: 12; -fx-text-fill: var(--muted);");

        Label riskBadge = new Label(response.risk.toUpperCase());
        riskBadge.setStyle(
                "-fx-font-size: 18;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 12 20;" +
                "-fx-background-color: " + response.getRiskColor() + ";" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 10 20;"
        );

        riskContainer.getChildren().addAll(riskLabel, riskBadge);

        // Score
        VBox scoreContainer = new VBox();
        scoreContainer.setSpacing(8);

        Label scoreLabel = new Label("Risk Score");
        scoreLabel.setStyle("-fx-font-size: 12; -fx-text-fill: var(--muted);");

        Label scoreText = new Label("Score: " + response.score);
        scoreText.setStyle("-fx-font-size: 14; -fx-text-fill: var(--ink);");

        scoreContainer.getChildren().addAll(scoreLabel, scoreText);

        // Flags
        VBox flagsContainer = new VBox();
        flagsContainer.setSpacing(8);

        Label flagsLabel = new Label("Detection Flags");
        flagsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: var(--muted);");

        if (response.flags.isEmpty()) {
            Label noFlags = new Label("No threats detected");
            noFlags.setStyle("-fx-font-size: 12; -fx-text-fill: var(--safe);");
            flagsContainer.getChildren().addAll(flagsLabel, noFlags);
        } else {
            flagsContainer.getChildren().add(flagsLabel);
            for (String flag : response.flags) {
                Label flagItem = new Label("• " + flag);
                flagItem.setStyle("-fx-font-size: 12; -fx-text-fill: var(--ink); -fx-wrap-text: true;");
                flagsContainer.getChildren().add(flagItem);
            }
        }

        resultPanel.getChildren().addAll(riskContainer, new Separator(), scoreContainer, new Separator(), flagsContainer);
        resultPanel.setVisible(true);

        scanButton.setDisable(false);
        scanButton.setText("Scan URL");
    }

    public VBox getNode() {
        return node;
    }
}
