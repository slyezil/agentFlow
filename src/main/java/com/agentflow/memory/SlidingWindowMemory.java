package com.agentflow.memory;

import com.agentflow.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility that trims conversation history to fit within a configurable
 * sliding window. This prevents exceeding the LLM's context window.
 */
public class SlidingWindowMemory {

    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowMemory.class);

    private final int maxMessages;

    public SlidingWindowMemory(
            @Value("${memory.max-messages:20}") int maxMessages) {
        this.maxMessages = maxMessages;
        logger.info("SlidingWindowMemory initialized with max-messages={}", maxMessages);
    }

    /**
     * Returns a trimmed view of the history, keeping only the most recent messages.
     * Always preserves the system prompt (handled separately by the caller).
     *
     * @param history The full conversation history
     * @return A trimmed list of messages within the window
     */
    public List<Message> trim(List<Message> history) {
        if (history.size() <= maxMessages) {
            return history;
        }

        logger.info("Trimming conversation from {} to {} messages", history.size(), maxMessages);
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    public int getMaxMessages() {
        return maxMessages;
    }
}
