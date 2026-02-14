package com.agentflow.config;

import com.agentflow.memory.ConversationMemory;
import com.agentflow.memory.FileConversationMemory;
import com.agentflow.memory.InMemoryConversationMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class that selects the ConversationMemory implementation
 * based on the 'memory.type' property in application.properties.
 *
 * Supported values:
 *   - "in-memory" (default): Volatile, fast, good for development.
 *   - "file":                Persistent, JSON-based, survives restarts.
 */
@Configuration
public class MemoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(MemoryConfig.class);

    @Bean
    @Primary
    public ConversationMemory conversationMemory(
            @Value("${memory.type:in-memory}") String memoryType,
            @Value("${memory.data-dir:./data}") String dataDir) {

        return switch (memoryType.toLowerCase()) {
            case "file" -> {
                logger.info("Using file-based ConversationMemory (data-dir={})", dataDir);
                yield new FileConversationMemory(dataDir);
            }
            default -> {
                logger.info("Using in-memory ConversationMemory");
                yield new InMemoryConversationMemory();
            }
        };
    }
}
