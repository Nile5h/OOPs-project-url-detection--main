package com.url_detector.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Features section displaying key capabilities.
 */
public class FeaturesSection {
    private VBox node;

    public FeaturesSection() {
        buildUI();
    }

    private void buildUI() {
        node = new VBox();
        node.setStyle("-fx-background-color: var(--bg); -fx-padding: 60 40;");
        node.setSpacing(40);

        // Section header
        Label sectionTitle = new Label("Features");
        sectionTitle.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: var(--ink);");

        Label sectionSubtitle = new Label("Built for educators, researchers, and security-conscious users.");
        sectionSubtitle.setStyle("-fx-font-size: 14; -fx-text-fill: var(--muted);");

        VBox header = new VBox();
        header.setSpacing(5);
        header.getChildren().addAll(sectionTitle, sectionSubtitle);

        // Features grid
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);
        grid.setPrefWidth(1000);

        String[][] features = {
                {
                        "Risk Classification",
                        "Instant SAFE to CRITICAL classification based on weighted heuristic and dataset-backed checks."
                },
                {
                        "Explainable Signals",
                        "Every verdict includes transparent indicators so results can be discussed clearly in class or review."
                },
                {
                        "Traceable History",
                        "Stored scan records with filter controls make verification and comparative analysis straightforward."
                },
                {
                        "Real-time Detection",
                        "Multi-stage checker pipeline evaluates URLs against phishing datasets, malicious lists, and structure analysis."
                },
                {
                        "Educational Focus",
                        "Designed for classroom demonstrations and teaching cybersecurity awareness without overwhelming complexity."
                },
                {
                        "Open Architecture",
                        "Modular design makes it easy to add new detection strategies and customize the risk scoring algorithm."
                }
        };

        int row = 0;
        for (int i = 0; i < features.length; i++) {
            VBox featureCard = createFeatureCard(features[i][0], features[i][1]);
            grid.add(featureCard, i % 3, i / 3);
        }

        node.getChildren().addAll(header, grid);
    }

    private VBox createFeatureCard(String title, String description) {
        VBox card = new VBox();
        card.setStyle(
                "-fx-background-color: var(--surface);" +
                "-fx-border-color: var(--line);" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 24;"
        );
        card.setSpacing(12);
        card.setPrefHeight(180);

        // Icon placeholder (using text)
        Label icon = new Label("■");
        icon.setStyle(
                "-fx-font-size: 24;" +
                "-fx-text-fill: var(--brand);"
        );

        Label cardTitle = new Label(title);
        cardTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: var(--ink);");

        Label cardDesc = new Label(description);
        cardDesc.setStyle(
                "-fx-font-size: 12;" +
                "-fx-text-fill: var(--muted);" +
                "-fx-wrap-text: true;"
        );

        card.getChildren().addAll(icon, cardTitle, cardDesc);
        return card;
    }

    public VBox getNode() {
        return node;
    }
}
