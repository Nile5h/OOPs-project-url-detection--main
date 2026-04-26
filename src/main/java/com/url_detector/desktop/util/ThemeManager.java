package com.url_detector.desktop.util;

import javafx.scene.Scene;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility for managing theme (light/dark mode) persistence and application.
 */
public class ThemeManager {
    private static final String THEME_CONFIG_DIR = ".url_detector";
    private static final String THEME_CONFIG_FILE = "theme.properties";
    private static final String THEME_KEY = "theme";
    private static final String DEFAULT_THEME = "light";

    private static String currentTheme = DEFAULT_THEME;

    static {
        loadTheme();
    }

    /**
     * Get the current theme (light or dark).
     */
    public static String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Set and persist the theme preference.
     */
    public static void setTheme(String theme) {
        if ("light".equals(theme) || "dark".equals(theme)) {
            currentTheme = theme;
            saveTheme();
        }
    }

    /**
     * Toggle between light and dark themes.
     */
    public static void toggleTheme() {
        currentTheme = "light".equals(currentTheme) ? "dark" : "light";
        saveTheme();
    }

    /**
     * Apply theme stylesheet to a scene.
     */
    public static void applyTheme(Scene scene) {
        String stylesheet = getStylesheet();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(stylesheet);
    }

    /**
     * Get the stylesheet URL for the current theme.
     */
    private static String getStylesheet() {
        return ThemeManager.class.getResource("/desktop-style.css").toExternalForm();
    }

    /**
     * Load theme preference from config file.
     */
    private static void loadTheme() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(configPath));
                String theme = props.getProperty(THEME_KEY, DEFAULT_THEME);
                if ("light".equals(theme) || "dark".equals(theme)) {
                    currentTheme = theme;
                }
            }
        } catch (IOException e) {
            // Use default theme if config file doesn't exist or can't be read
            currentTheme = DEFAULT_THEME;
        }
    }

    /**
     * Save theme preference to config file.
     */
    private static void saveTheme() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            
            Properties props = new Properties();
            props.setProperty(THEME_KEY, currentTheme);
            
            try (OutputStream os = Files.newOutputStream(configPath)) {
                props.store(os, "URL Detector Desktop Theme Configuration");
            }
        } catch (IOException e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }

    /**
     * Get the configuration file path in user home directory.
     */
    private static Path getConfigPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, THEME_CONFIG_DIR, THEME_CONFIG_FILE);
    }

    /**
     * Get CSS root color variable for the current theme.
     */
    public static String getCSSVariable(String variableName) {
        if ("light".equals(currentTheme)) {
            return getLightThemeVariable(variableName);
        } else {
            return getDarkThemeVariable(variableName);
        }
    }

    private static String getLightThemeVariable(String varName) {
        return switch (varName) {
            case "--bg" -> "#f4f7fb";
            case "--bg-deep" -> "#e8eef8";
            case "--surface" -> "#ffffff";
            case "--surface-2" -> "#f8fbff";
            case "--ink" -> "#0c1a2e";
            case "--muted" -> "#51607a";
            case "--line" -> "#d6deea";
            case "--brand" -> "#1259c3";
            case "--brand-2" -> "#0b89ae";
            case "--accent" -> "#00a389";
            case "--critical" -> "#b4232a";
            case "--high" -> "#d04b22";
            case "--medium" -> "#bf7a00";
            case "--low" -> "#2a8a37";
            case "--safe" -> "#1f7a4f";
            default -> "#ffffff";
        };
    }

    private static String getDarkThemeVariable(String varName) {
        return switch (varName) {
            case "--bg" -> "#091325";
            case "--bg-deep" -> "#121f3a";
            case "--surface" -> "#13243f";
            case "--surface-2" -> "#0f1d34";
            case "--ink" -> "#e8f0ff";
            case "--muted" -> "#b8c6df";
            case "--line" -> "#26446e";
            case "--brand" -> "#65a4ff";
            case "--brand-2" -> "#2bc2df";
            case "--accent" -> "#2fd2aa";
            case "--critical" -> "#ff7a85";
            case "--high" -> "#ff9a6e";
            case "--medium" -> "#ffca7a";
            case "--low" -> "#7fdb8d";
            case "--safe" -> "#79d9ac";
            default -> "#13243f";
        };
    }
}
