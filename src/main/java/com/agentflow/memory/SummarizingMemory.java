package com.agentflow.memory;

import com.agentflow.dto.Message;
import com.agentflow.interfaces.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced memory manager that summarizes older conversation context
 * instead of simply discarding it. When a conversation exceeds the
 * sliding window, older messages are compressed into a summary
 * that is prepended to the trimmed history.
 *
 * This preserves important context while staying within the model's
 * context window limits.
 */
@Component
public class SummarizingMemory implements MemoryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SummarizingMemory.class);

    private final int maxMessages;
    private final int summarizeThreshold;
    private final LlmClient llmClient;

    private static final String SUMMARIZE_PROMPT =
            "Summarize the following conversation in 2-3 concise sentences. " +
            "Focus on key facts, decisions, and any user preferences mentioned. " +
            "Do not add commentary, just summarize:\n\n";

    public SummarizingMemory(
            LlmClient llmClient,
            @Value("${memory.max-messages:20}") int maxMessages,
            @Value("${memory.summarize-threshold:30}") int summarizeThreshold) {
        this.llmClient = llmClient;
        this.maxMessages = maxMessages;
        this.summarizeThreshold = summarizeThreshold;
        logger.info("SummarizingMemory initialized (max-messages={}, summarize-threshold={})",
                maxMessages, summarizeThreshold);
    }

    /**
     * Processes history: if it exceeds the threshold, summarize the older
     * portion and return a compressed history that fits the window.
     *
     * @param history Full conversation history
     * @return Processed history with optional summary prepended
     */
    public List<Message> process(List<Message> history) {
        if (history.size() <= maxMessages) {
            return history;
        }

        // Only invoke LLM summarization if we've really accumulated a lot
        if (history.size() >= summarizeThreshold) {
            return summarizeAndTrim(history);
        }

        // Otherwise, just trim (same as SlidingWindowMemory)
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    private List<Message> summarizeAndTrim(List<Message> history) {
        // Split: older messages to summarize, recent messages to keep
        int keepFrom = history.size() - maxMessages;
        List<Message> olderMessages = history.subList(0, keepFrom);
        List<Message> recentMessages = history.subList(keepFrom, history.size());

        try {
            // Format older messages for summarization
            String conversationText = olderMessages.stream()
                    .map(m -> m.role().substring(0, 1).toUpperCase() + m.role().substring(1)
                              + ": " + m.content())
                    .collect(Collectors.joining("\n"));

            String summary = llmClient.generateRaw(SUMMARIZE_PROMPT + conversationText);

            if (summary != null && !summary.isBlank()) {
                logger.info("Generated conversation summary ({} chars) from {} older messages",
                        summary.length(), olderMessages.size());

                List<Message> result = new ArrayList<>();
                result.add(new Message("system",
                        "Summary of earlier conversation: " + summary));
                result.addAll(recentMessages);
                return result;
            }
        } catch (Exception e) {
            logger.warn("Failed to generate summary, falling back to simple trim: {}",
                    e.getMessage());
        }

        // Fallback: simple trim
        return new ArrayList<>(recentMessages);
    }
}
