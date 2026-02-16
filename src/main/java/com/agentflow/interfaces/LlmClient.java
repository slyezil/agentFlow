package com.agentflow.interfaces;

import com.agentflow.dto.Message;
import reactor.core.publisher.Flux;
import java.util.List;

public interface LlmClient {
    String generate(String prompt);

    /**
     * Generate a response using the full conversation history.
     * User preferences are automatically injected.
     */
    String generate(String systemPrompt, List<Message> history);

    /**
     * Raw generate without injecting user preferences.
     * Used for internal operations (e.g. summarization).
     */
    String generateRaw(String prompt);

    /**
     * Stream a response token-by-token using SSE.
     */
    Flux<String> generateStream(String systemPrompt, List<Message> history);
}
