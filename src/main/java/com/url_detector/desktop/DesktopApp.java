package com.url_detector.desktop;

import com.url_detector.desktop.ui.MainWindow;
import com.url_detector.desktop.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the URL Detector Desktop Application.
 * Run with: mvn javafx:run -Djavafx.mainClass=com.url_detector.desktop.DesktopApp
 * Or: java -jar url_detector-desktop.jar
 */
public class DesktopApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Create the main window
        MainWindow mainWindow = new MainWindow();
        javafx.scene.layout.BorderPane root = mainWindow.getRoot();

        // Create scene
        Scene scene = new Scene(root, 1200, 800);

        // Apply theme
        ThemeManager.applyTheme(scene);

        // Configure stage
        primaryStage.setTitle("PhishNet.AI - URL Threat Intelligence");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Show the application
        primaryStage.show();

        // Start health check on app load
        mainWindow.startHealthCheck();
    }
}
