package com.url_detector.desktop;

import com.url_detector.desktop.model.AnalyzeResponse;
import com.url_detector.desktop.model.HistoryRecord;
import com.url_detector.desktop.service.ApiClient;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DesktopApp extends Application {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String PREF_FILE = ".url-detector-desktop.properties";

    private final ApiClient apiClient = new ApiClient(BASE_URL);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ObservableList<HistoryRecord> historyItems = FXCollections.observableArrayList();

    private final Label healthStatus = new Label("Checking API...");
    private final Button themeToggle = new Button("Dark mode");
    private final Label errorLabel = new Label();
    private final Label riskBadge = new Label("Not scanned");
    private final Label scoreLabel = new Label("Score: -");
    private final ListView<String> flagsView = new ListView<>();

    private final TextField urlInput = new TextField();
    private final Button scanButton = new Button("Check URL");
    private final ComboBox<String> riskFilter = new ComboBox<>();

    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox content = new VBox(24);
    private final Map<String, Region> sections = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    private final Button copyResultButton = new Button("Copy Result");
    private final Button copyFlagsButton = new Button("Copy Flags");

    private Timeline healthTimeline;
    private String currentTheme = "light";
    private String latestAnalyzedUrl = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        HBox topBar = buildTopBar();
        root.setTop(topBar);

        configureMainContent();
        root.setCenter(scrollPane);

        Label footer = new Label("PhishNet.AI Desktop • Built with JavaFX");
        footer.getStyleClass().add("footer");
        BorderPane.setMargin(footer, new Insets(10, 20, 18, 20));
        root.setBottom(footer);

        Scene scene = new Scene(root, 1200, 820);
        String css = getClass().getResource("/desktop-style.css") == null
            ? null
            : getClass().getResource("/desktop-style.css").toExternalForm();
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        configureShortcuts(scene);

        loadThemePreference();
        applyTheme(root);
        themeToggle.setOnAction(evt -> {
            currentTheme = "dark".equals(currentTheme) ? "light" : "dark";
            applyTheme(root);
            saveThemePreference();
        });

        primaryStage.setTitle("PhishNet.AI Desktop");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();

        setActiveNav("overview");

        startHealthPolling();
        loadHistory();
    }

    @Override
    public void stop() {
        if (healthTimeline != null) {
            healthTimeline.stop();
        }
        executor.shutdownNow();
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("topbar");
        topBar.setPadding(new Insets(12, 18, 12, 18));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("PhishNet.AI");
        brand.getStyleClass().add("brand");

        HBox nav = new HBox(8);
        nav.getStyleClass().add("nav");

        nav.getChildren().add(navButton("Overview", "overview"));
        nav.getChildren().add(navButton("Analyze", "analyze"));
        nav.getChildren().add(navButton("History", "history"));
        nav.getChildren().add(navButton("Features", "features"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        healthStatus.getStyleClass().add("status-pill");
        themeToggle.getStyleClass().add("theme-toggle");

        topBar.getChildren().addAll(brand, nav, spacer, healthStatus, themeToggle);
        return topBar;
    }

    private Button navButton(String text, String sectionId) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-link");
        navButtons.put(sectionId, button);
        button.setOnAction(evt -> {
            Region target = sections.get(sectionId);
            if (target != null) {
                setActiveNav(sectionId);
                scrollTo(target);
            }
        });
        return button;
    }

    private void configureMainContent() {
        content.getStyleClass().add("content");
        content.setPadding(new Insets(18, 20, 20, 20));

        Region overview = buildOverviewSection();
        Region features = buildFeaturesSection();
        Region analyze = buildAnalyzeSection();
        Region history = buildHistorySection();

        sections.put("overview", overview);
        sections.put("features", features);
        sections.put("analyze", analyze);
        sections.put("history", history);

        content.getChildren().addAll(overview, features, analyze, history);

        scrollPane.setFitToWidth(true);
        scrollPane.setContent(content);
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> updateActiveSectionFromScroll());
    }

    private Region buildOverviewSection() {
        HBox hero = new HBox(18);
        hero.getStyleClass().addAll("card", "hero");
        hero.setPadding(new Insets(24));

        VBox copy = new VBox(10);
        Label kicker = new Label("Educational URL Safety Platform");
        kicker.getStyleClass().add("kicker");

        Label title = new Label("Professional Phishing Detection for Safer Browsing");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);

        Label subtitle = new Label("Evaluate suspicious links with risk scoring, transparent signals, and auditable history logs.");
        subtitle.getStyleClass().add("subtitle");
        subtitle.setWrapText(true);

        Button cta = new Button("Check URL");
        cta.getStyleClass().add("btn-primary");
        cta.setOnAction(evt -> {
            Region analyze = sections.get("analyze");
            if (analyze != null) {
                scrollTo(analyze);
            }
        });

        copy.getChildren().addAll(kicker, title, subtitle, cta);
        HBox.setHgrow(copy, Priority.ALWAYS);

        StackPane media = new StackPane();
        media.getStyleClass().add("hero-media");
        media.setMinWidth(260);
        media.setPrefWidth(330);

        ImageView imageView = new ImageView();
        try (InputStream in = getClass().getResourceAsStream("/static/assets/images.png")) {
            if (in != null) {
                Image image = new Image(in);
                imageView.setImage(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(300);
            }
        } catch (IOException ignored) {
            // Keep section functional even when the asset cannot be loaded.
        }

        media.getChildren().add(imageView);
        hero.getChildren().addAll(copy, media);
        return hero;
    }

    private Region buildFeaturesSection() {
        VBox wrap = new VBox(12);
        wrap.getStyleClass().addAll("card", "section");
        wrap.setPadding(new Insets(22));

        Label title = new Label("Features");
        title.getStyleClass().add("section-title");

        HBox row = new HBox(12);
        row.getChildren().add(featureCard("Risk Classification", "Instant SAFE to CRITICAL classification backed by weighted checks."));
        row.getChildren().add(featureCard("Explainable Signals", "Each verdict includes transparent indicators for classroom review."));
        row.getChildren().add(featureCard("Traceable History", "Stored scans with filtering make reports and comparison easier."));

        wrap.getChildren().addAll(title, row);
        return wrap;
    }

    private VBox featureCard(String title, String detail) {
        VBox card = new VBox(8);
        card.getStyleClass().add("feature-card");
        card.setPadding(new Insets(14));
        HBox.setHgrow(card, Priority.ALWAYS);

        Label t = new Label(title);
        t.getStyleClass().add("feature-title");
        t.setWrapText(true);

        Label d = new Label(detail);
        d.getStyleClass().add("feature-detail");
        d.setWrapText(true);

        card.getChildren().addAll(t, d);
        return card;
    }

    private Region buildAnalyzeSection() {
        VBox panel = new VBox(12);
        panel.getStyleClass().addAll("card", "section");
        panel.setPadding(new Insets(22));

        Label title = new Label("Analyze URL");
        title.getStyleClass().add("section-title");

        urlInput.setPromptText("https://example.com/login");
        urlInput.getStyleClass().add("url-input");
        HBox.setHgrow(urlInput, Priority.ALWAYS);

        scanButton.getStyleClass().add("btn-primary");
        scanButton.setOnAction(evt -> analyzeUrl());
        scanButton.setTooltip(new Tooltip("Ctrl+Enter"));

        urlInput.setOnAction(evt -> analyzeUrl());
        urlInput.setTooltip(new Tooltip("Ctrl+L to focus"));

        HBox row = new HBox(10, urlInput, scanButton);

        Label shortcutHint = new Label("Shortcuts: Ctrl+Enter to scan, Ctrl+L to focus URL");
        shortcutHint.getStyleClass().add("shortcut-hint");

        errorLabel.getStyleClass().add("error-text");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox resultPanel = new VBox(8);
        resultPanel.getStyleClass().add("result-panel");
        riskBadge.getStyleClass().add("risk-badge");
        flagsView.setPlaceholder(new Label("No suspicious signals detected."));

        copyResultButton.getStyleClass().add("btn-secondary");
        copyFlagsButton.getStyleClass().add("btn-secondary");
        copyResultButton.setDisable(true);
        copyFlagsButton.setDisable(true);
        copyResultButton.setOnAction(evt -> copyResultSummary());
        copyFlagsButton.setOnAction(evt -> copyFlags());

        HBox resultActions = new HBox(10, copyResultButton, copyFlagsButton);
        resultPanel.getChildren().addAll(riskBadge, scoreLabel, flagsView, resultActions);

        panel.getChildren().addAll(title, row, shortcutHint, errorLabel, resultPanel);
        return panel;
    }

    private Region buildHistorySection() {
        VBox panel = new VBox(12);
        panel.getStyleClass().addAll("card", "section");
        panel.setPadding(new Insets(22));

        Label title = new Label("Scan History");
        title.getStyleClass().add("section-title");

        riskFilter.setItems(FXCollections.observableArrayList("ALL", "SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL"));
        riskFilter.getSelectionModel().select("ALL");
        riskFilter.setOnAction(evt -> loadHistory());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("btn-secondary");
        refresh.setOnAction(evt -> loadHistory());

        Button clear = new Button("Clear History");
        clear.getStyleClass().add("btn-danger");
        clear.setOnAction(evt -> clearHistory());

        HBox controls = new HBox(10, riskFilter, refresh, clear);

        TableView<HistoryRecord> table = new TableView<>(historyItems);
        table.setPlaceholder(new Label("No scans yet."));
        table.setRowFactory(tv -> {
            TableRow<HistoryRecord> row = new TableRow<>();
            MenuItem copyUrl = new MenuItem("Copy URL");
            copyUrl.setOnAction(evt -> {
                HistoryRecord record = row.getItem();
                if (record != null) {
                    copyToClipboard(record.getUrl());
                }
            });

            MenuItem copyRow = new MenuItem("Copy Row Summary");
            copyRow.setOnAction(evt -> {
                HistoryRecord record = row.getItem();
                if (record != null) {
                    copyToClipboard("Time: " + safe(record.getScannedAt())
                        + " | Risk: " + safe(record.getRiskLevel())
                        + " | Score: " + record.getScore()
                        + " | URL: " + safe(record.getUrl())
                        + " | Flags: " + safe(record.getFlagsText()));
                }
            });

            ContextMenu menu = new ContextMenu(copyUrl, copyRow);
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> row.setContextMenu(isNowEmpty ? null : menu));
            return row;
        });

        TableColumn<HistoryRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("scannedAt"));
        timeCol.setPrefWidth(185);

        TableColumn<HistoryRecord, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlCol.setPrefWidth(360);

        TableColumn<HistoryRecord, String> riskCol = new TableColumn<>("Risk");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        riskCol.setPrefWidth(95);

        TableColumn<HistoryRecord, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(90);

        TableColumn<HistoryRecord, String> flagsCol = new TableColumn<>("Flags");
        flagsCol.setCellValueFactory(new PropertyValueFactory<>("flagsText"));
        flagsCol.setPrefWidth(340);

        table.getColumns().add(timeCol);
        table.getColumns().add(urlCol);
        table.getColumns().add(riskCol);
        table.getColumns().add(scoreCol);
        table.getColumns().add(flagsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(280);

        panel.getChildren().addAll(title, controls, table);
        return panel;
    }

    private void analyzeUrl() {
        hideError();
        String rawUrl = urlInput.getText() == null ? "" : urlInput.getText().trim();
        if (rawUrl.isBlank()) {
            showError("Please enter a URL.");
            return;
        }

        scanButton.setDisable(true);
        scanButton.setText("Scanning...");

        executor.submit(() -> {
            try {
                AnalyzeResponse response = apiClient.analyzeUrl(rawUrl);
                Platform.runLater(() -> {
                    updateResult(response);
                    loadHistory();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    scanButton.setDisable(false);
                    scanButton.setText("Check URL");
                });
            }
        });
    }

    private void updateResult(AnalyzeResponse response) {
        clearRiskClasses();
        String risk = response.getRiskLevel() == null ? "UNKNOWN" : response.getRiskLevel();
        latestAnalyzedUrl = response.getUrl() == null || response.getUrl().isBlank() ? urlInput.getText() : response.getUrl();
        riskBadge.setText(risk);
        riskBadge.getStyleClass().add("risk-" + risk.toLowerCase());
        scoreLabel.setText("Score: " + response.getScore());

        List<String> flags = response.getFlags() == null ? new ArrayList<>() : response.getFlags();
        if (flags.isEmpty()) {
            flagsView.setItems(FXCollections.observableArrayList("No suspicious signals detected."));
        } else {
            flagsView.setItems(FXCollections.observableArrayList(flags));
        }

        copyResultButton.setDisable(false);
        copyFlagsButton.setDisable(false);
    }

    private void loadHistory() {
        String selectedRisk = riskFilter.getSelectionModel().getSelectedItem();
        executor.submit(() -> {
            try {
                List<HistoryRecord> records = apiClient.loadHistory(selectedRisk, 30);
                Platform.runLater(() -> historyItems.setAll(records));
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        });
    }

    private void clearHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm clear");
        confirm.setHeaderText("Clear all scan history?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(result -> {
            if (result.getButtonData().isCancelButton()) {
                return;
            }

            executor.submit(() -> {
                try {
                    apiClient.clearHistory();
                    Platform.runLater(this::loadHistory);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            });
        });
    }

    private void startHealthPolling() {
        Runnable poll = () -> executor.submit(() -> {
            try {
                boolean online = apiClient.checkHealth();
                Platform.runLater(() -> {
                    healthStatus.setText(online ? "API Online" : "API Offline");
                    healthStatus.getStyleClass().removeAll("status-online", "status-offline");
                    healthStatus.getStyleClass().add(online ? "status-online" : "status-offline");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    healthStatus.setText("API Offline");
                    healthStatus.getStyleClass().removeAll("status-online", "status-offline");
                    healthStatus.getStyleClass().add("status-offline");
                });
            }
        });

        poll.run();
        healthTimeline = new Timeline(new KeyFrame(Duration.seconds(5), evt -> poll.run()));
        healthTimeline.setCycleCount(Timeline.INDEFINITE);
        healthTimeline.play();
    }

    private void clearRiskClasses() {
        riskBadge.getStyleClass().removeAll(
            "risk-safe", "risk-low", "risk-medium", "risk-high", "risk-critical", "risk-unknown"
        );
    }

    private void scrollTo(Region target) {
        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double max = Math.max(contentHeight - viewportHeight, 1);
        double targetY = Math.min(target.getBoundsInParent().getMinY(), max);
        double v = targetY / max;

        Timeline smooth = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(scrollPane.vvalueProperty(), scrollPane.getVvalue())),
            new KeyFrame(Duration.millis(260), new KeyValue(scrollPane.vvalueProperty(), v))
        );
        smooth.play();
    }

    private void updateActiveSectionFromScroll() {
        if (sections.isEmpty()) {
            return;
        }

        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double offsetY = scrollPane.getVvalue() * Math.max(contentHeight - viewportHeight, 0);
        double middleY = offsetY + (viewportHeight * 0.35);

        String winner = "overview";
        double nearest = Double.MAX_VALUE;

        for (Map.Entry<String, Region> entry : sections.entrySet()) {
            double sectionY = entry.getValue().getBoundsInParent().getMinY();
            double distance = Math.abs(sectionY - middleY);
            if (distance < nearest) {
                nearest = distance;
                winner = entry.getKey();
            }
        }

        setActiveNav(winner);
    }

    private void setActiveNav(String sectionId) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            Button button = entry.getValue();
            button.getStyleClass().remove("nav-link-active");
            if (entry.getKey().equals(sectionId)) {
                button.getStyleClass().add("nav-link-active");
            }
        }
    }

    private void configureShortcuts(Scene scene) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
            this::analyzeUrl
        );

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
            () -> Platform.runLater(() -> {
                urlInput.requestFocus();
                urlInput.selectAll();
            })
        );
    }

    private void copyResultSummary() {
        String summary = "URL: " + safe(latestAnalyzedUrl)
            + "\nRisk: " + riskBadge.getText()
            + "\n" + scoreLabel.getText();
        copyToClipboard(summary);
    }

    private void copyFlags() {
        List<String> items = flagsView.getItems();
        if (items == null || items.isEmpty()) {
            copyToClipboard("No suspicious signals detected.");
            return;
        }
        copyToClipboard(String.join("\n", items));
    }

    private void copyToClipboard(String text) {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(safe(text));
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        errorLabel.setText(message == null || message.isBlank() ? "Request failed" : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void applyTheme(BorderPane root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add("dark".equals(currentTheme) ? "theme-dark" : "theme-light");
        themeToggle.setText("dark".equals(currentTheme) ? "Light mode" : "Dark mode");
    }

    private void loadThemePreference() {
        Path file = preferencePath();
        if (!Files.exists(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            String value = properties.getProperty("theme", "light");
            currentTheme = "dark".equalsIgnoreCase(value) ? "dark" : "light";
        } catch (IOException ignored) {
            currentTheme = "light";
        }
    }

    private void saveThemePreference() {
        Path file = preferencePath();
        Properties properties = new Properties();
        properties.setProperty("theme", currentTheme);

        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "URL Detector Desktop Preferences");
        } catch (IOException ignored) {
            // Ignore persistence failures to keep app behavior resilient.
        }
    }

    private Path preferencePath() {
        String home = System.getProperty("user.home", ".");
        return Path.of(home, PREF_FILE);
    }
}
