package com.url_detector.desktop.ui;

import com.url_detector.desktop.service.ApiClient;
import com.url_detector.desktop.util.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Main window controller that manages the overall layout and coordinates UI sections.
 */
public class MainWindow {
    private BorderPane root;
    private Label healthStatusLabel;
    private OverviewSection overviewSection;
    private AnalyzeSection analyzeSection;
    private HistorySection historySection;
    private FeaturesSection featuresSection;

    public MainWindow() {
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: var(--bg);");

        // Top: Header with navigation
        root.setTop(buildHeader());

        // Center: Main content with sections
        root.setCenter(buildMainContent());

        // Bottom: Footer
        root.setBottom(buildFooter());
    }

    private VBox buildHeader() {
        VBox header = new VBox();
        header.setStyle(
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-width: 0 0 1 0;" +
                "-fx-padding: 12 20;"
        );
        header.setPrefHeight(70);

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setSpacing(20);

        // Brand
        HBox brand = new HBox();
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setSpacing(8);
        Label brandLabel = new Label("PhishNet.AI");
        brandLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: var(--brand);");
        brand.getChildren().add(brandLabel);

        // Navigation
        HBox nav = new HBox();
        nav.setSpacing(0);
        nav.setPrefWidth(400);

        String[] navItems = {"Overview", "Analyze", "History", "Features"};
        for (String item : navItems) {
            Button navBtn = createNavButton(item);
            nav.getChildren().add(navBtn);
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status and Theme Toggle
        HBox rightTools = new HBox();
        rightTools.setAlignment(Pos.CENTER_RIGHT);
        rightTools.setSpacing(15);

        healthStatusLabel = new Label("Checking API...");
        healthStatusLabel.setStyle("-fx-text-fill: var(--muted); -fx-font-size: 12;");
        Pane statusPill = new Pane(healthStatusLabel);
        statusPill.setStyle(
                "-fx-background-color: var(--surface-2);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 6 12;"
        );

        Button themeToggle = new Button("Dark Mode");
        themeToggle.setStyle(
                "-fx-padding: 8 12;" +
                "-fx-background-color: var(--surface-2);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 11;"
        );
        themeToggle.setOnAction(e -> {
            ThemeManager.toggleTheme();
            themeToggle.setText("Light Mode".equals(ThemeManager.getCurrentTheme()) ? "Dark Mode" : "Light Mode");
            // Note: Full theme switching requires scene reference - handled in DesktopApp
        });

        rightTools.getChildren().addAll(statusPill, themeToggle);

        topBar.getChildren().addAll(brand, nav, spacer, rightTools);
        header.getChildren().add(topBar);

        return header;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-padding: 8 16;" +
                "-fx-background-color: transparent;" +
                "-fx-text-fill: var(--ink);" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 0 0 3 0;"
        );
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("-fx-border-color: var(--brand);")) {
                btn.setStyle(btn.getStyle().replace("-fx-background-color: transparent;", "-fx-background-color: var(--surface-2);"));
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("-fx-border-color: var(--brand);")) {
                btn.setStyle(btn.getStyle().replace("-fx-background-color: var(--surface-2);", "-fx-background-color: transparent;"));
            }
        });
        return btn;
    }

    private ScrollPane buildMainContent() {
        VBox content = new VBox();
        content.setStyle("-fx-background-color: var(--bg);");
        content.setSpacing(0);

        // Create sections
        overviewSection = new OverviewSection();
        analyzeSection = new AnalyzeSection();
        historySection = new HistorySection();
        featuresSection = new FeaturesSection();

        // Add sections to content
        content.getChildren().addAll(
                overviewSection.getNode(),
                analyzeSection.getNode(),
                historySection.getNode(),
                featuresSection.getNode()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: var(--bg);");
        scrollPane.getStyleClass().add("edge-to-edge");

        return scrollPane;
    }

    private VBox buildFooter() {
        VBox footer = new VBox();
        footer.setStyle(
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-width: 1 0 0 0;" +
                "-fx-padding: 20;" +
                "-fx-alignment: center;"
        );

        Label footerText = new Label("PhishNet.AI © 2024 | Educational URL Safety Platform");
        footerText.setStyle("-fx-text-fill: var(--muted); -fx-font-size: 11;");

        footer.getChildren().add(footerText);
        return footer;
    }

    public BorderPane getRoot() {
        return root;
    }

    public void startHealthCheck() {
        // Perform periodic health checks
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                    var health = ApiClient.checkHealth();
                    Platform.runLater(() -> {
                        if (health.isHealthy()) {
                            healthStatusLabel.setText("✓ Connected");
                            healthStatusLabel.setStyle("-fx-text-fill: var(--safe); -fx-font-size: 12;");
                        } else {
                            healthStatusLabel.setText("✗ Offline");
                            healthStatusLabel.setStyle("-fx-text-fill: var(--critical); -fx-font-size: 12;");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        healthStatusLabel.setText("✗ Offline");
                        healthStatusLabel.setStyle("-fx-text-fill: var(--critical); -fx-font-size: 12;");
                    });
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        }).setDaemon(true);
    }
}
