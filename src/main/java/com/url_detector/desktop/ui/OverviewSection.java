package com.url_detector.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Overview section with hero image and feature highlights.
 */
public class OverviewSection {
    private VBox node;

    public OverviewSection() {
        buildUI();
    }

    private void buildUI() {
        node = new VBox();
        node.setStyle("-fx-background-color: var(--bg); -fx-padding: 60 40;");
        node.setSpacing(40);

        // Hero content
        HBox heroContainer = new HBox();
        heroContainer.setSpacing(60);
        heroContainer.setAlignment(Pos.CENTER_LEFT);

        // Left side: Copy
        VBox heroCopy = new VBox();
        heroCopy.setSpacing(20);
        heroCopy.setPrefWidth(400);

        Label kicker = new Label("Educational URL Safety Platform");
        kicker.setStyle("-fx-font-size: 12; -fx-text-fill: var(--brand); -fx-font-weight: bold;");

        Label title = new Label("Professional Phishing Detection\nfor Safer Browsing");
        title.setStyle("-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: var(--ink); -fx-wrap-text: true;");

        Label subtitle = new Label(
                "Evaluate suspicious links with risk scoring, transparent signals, and auditable history logs.\n" +
                "Built for demonstrations, coursework, and real-world URL awareness."
        );
        subtitle.setStyle("-fx-font-size: 14; -fx-text-fill: var(--muted); -fx-wrap-text: true;");

        // CTA buttons
        HBox ctaButtons = new HBox();
        ctaButtons.setSpacing(15);

        Button primaryCTA = new Button("Check URL");
        primaryCTA.setStyle(
                "-fx-padding: 12 24;" +
                "-fx-background-color: var(--brand);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );

        Button secondaryCTA = new Button("View Features");
        secondaryCTA.setStyle(
                "-fx-padding: 12 24;" +
                "-fx-background-color: var(--surface-2);" +
                "-fx-text-fill: var(--ink);" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: var(--line);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );

        ctaButtons.getChildren().addAll(primaryCTA, secondaryCTA);

        // Trust indicators
        VBox trustList = new VBox();
        trustList.setSpacing(10);

        Label trustItem1 = new Label("✓ Signal-based scoring with explainable flags");
        trustItem1.setStyle("-fx-font-size: 12; -fx-text-fill: var(--ink);");

        Label trustItem2 = new Label("✓ Structured scan history for reporting and review");
        trustItem2.setStyle("-fx-font-size: 12; -fx-text-fill: var(--ink);");

        trustList.getChildren().addAll(trustItem1, trustItem2);

        heroCopy.getChildren().addAll(kicker, title, subtitle, ctaButtons, trustList);

        // Right side: Image
        VBox heroMedia = new VBox();
        heroMedia.setAlignment(Pos.CENTER);
        heroMedia.setPrefWidth(400);
        heroMedia.setPrefHeight(350);

        try {
            Image image = new Image(getClass().getResourceAsStream("/static/assets/images.png"));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(400);
            imageView.setFitHeight(350);
            imageView.setPreserveRatio(true);
            heroMedia.getChildren().add(imageView);
        } catch (Exception e) {
            // Fallback if image not found
            Label fallback = new Label("Dashboard Image");
            fallback.setStyle("-fx-font-size: 14; -fx-text-fill: var(--muted); -fx-padding: 100;");
            heroMedia.getChildren().add(fallback);
        }

        heroContainer.getChildren().addAll(heroCopy, heroMedia);

        node.getChildren().add(heroContainer);
    }

    public VBox getNode() {
        return node;
    }
}
