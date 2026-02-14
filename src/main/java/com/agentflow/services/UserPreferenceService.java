package com.agentflow.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferenceService.class);

    private final Set<String> preferences = Collections.synchronizedSet(new HashSet<>());
    private final Path preferencesFile;
    private final ObjectMapper objectMapper;

    // Simple keyword-based extraction patterns
    private static final Pattern[] PREFERENCE_PATTERNS = {
            Pattern.compile("(?i)I like (.*)"),
            Pattern.compile("(?i)I prefer (.*)"),
            Pattern.compile("(?i)I love (.*)"),
            Pattern.compile("(?i)My favorite (.*) is (.*)"),
            Pattern.compile("(?i)Remind me that (.*)")
    };

    public UserPreferenceService(
            @Value("${memory.data-dir:./data}") String dataDir) {
        this.preferencesFile = Paths.get(dataDir, "preferences.json");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadPreferences();
    }

    public void extractPreferences(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        boolean changed = false;
        for (Pattern pattern : PREFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String match;
                if (matcher.groupCount() == 2) {
                    match = matcher.group(1) + " is " + matcher.group(2);
                } else {
                    match = matcher.group(1);
                }

                // Clean up the match (remove punctuation at the end)
                match = match.replaceAll("[.!?]$", "").trim();

                if (!match.isEmpty() && preferences.add(match)) {
                    logger.info("Extracted user preference: {}", match);
                    changed = true;
                }
            }
        }

        if (changed) {
            savePreferences();
        }
    }

    public Set<String> getPreferences() {
        return new HashSet<>(preferences);
    }

    public String getPreferencesPrompt() {
        if (preferences.isEmpty()) {
            return "";
        }

        String prefs = preferences.stream()
                .map(p -> "- " + p)
                .collect(Collectors.joining("\n"));

        return "The user has shared the following preferences and information about themselves:\n" + prefs + "\n\nUse this information to personalize your responses when relevant.";
    }

    public void clearPreferences() {
        preferences.clear();
        savePreferences();
    }

    private void loadPreferences() {
        File file = preferencesFile.toFile();
        if (!file.exists()) {
            logger.info("No existing preferences file found at {}", preferencesFile);
            return;
        }

        try {
            Set<String> loaded = objectMapper.readValue(file, new TypeReference<Set<String>>() {});
            preferences.addAll(loaded);
            logger.info("Loaded {} preferences from disk", preferences.size());
        } catch (IOException e) {
            logger.warn("Could not load preferences from {}: {}", preferencesFile, e.getMessage());
        }
    }

    private void savePreferences() {
        try {
            Files.createDirectories(preferencesFile.getParent());
            objectMapper.writeValue(preferencesFile.toFile(), preferences);
            logger.debug("Saved {} preferences to disk", preferences.size());
        } catch (IOException e) {
            logger.error("Failed to save preferences to {}: {}", preferencesFile, e.getMessage());
        }
    }
}
