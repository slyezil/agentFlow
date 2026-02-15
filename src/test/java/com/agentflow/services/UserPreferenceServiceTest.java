package com.agentflow.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPreferenceServiceTest {

    private UserPreferenceService userPreferenceService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        userPreferenceService = new UserPreferenceService(tempDir.toString());
    }

    @Test
    void testExtractPreferences() {
        userPreferenceService.extractPreferences("I like pizza and pasta.");
        userPreferenceService.extractPreferences("I prefer dark mode.");
        userPreferenceService.extractPreferences("My favorite color is blue!");
        userPreferenceService.extractPreferences("Remind me that I have a meeting at 2pm.");

        Set<String> preferences = userPreferenceService.getPreferences();
        assertEquals(4, preferences.size());
        assertTrue(preferences.contains("pizza and pasta"));
        assertTrue(preferences.contains("dark mode"));
        assertTrue(preferences.contains("color is blue"));
        assertTrue(preferences.contains("I have a meeting at 2pm"));
    }

    @Test
    void testGetPreferencesPrompt() {
        userPreferenceService.extractPreferences("I like coffee.");
        
        String prompt = userPreferenceService.getPreferencesPrompt();
        assertTrue(prompt.contains("The user has shared the following preferences"));
        assertTrue(prompt.contains("- coffee"));
    }

    @Test
    void testEmptyPreferences() {
        assertEquals("", userPreferenceService.getPreferencesPrompt());
        assertTrue(userPreferenceService.getPreferences().isEmpty());
    }

    @Test
    void testClearPreferences() {
        userPreferenceService.extractPreferences("I like coffee.");
        userPreferenceService.clearPreferences();
        assertTrue(userPreferenceService.getPreferences().isEmpty());
    }
}
