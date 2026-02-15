package com.agentflow.interfaces;

import com.agentflow.dto.Message;
import reactor.core.publisher.Flux;
import java.util.List;

public interface LlmClient {
    String generate(String prompt);

    /**
     * Generate a response using the full conversation history.
     *
     * @param systemPrompt The system prompt (can be null)
     * @param history      The conversation history
     * @return The LLM's response
     */
    String generate(String systemPrompt, List<Message> history);

    /**
     * Stream a response token-by-token using SSE.
     *
     * @param systemPrompt The system prompt (can be null)
     * @param history      The conversation history
     * @return A Flux emitting content tokens as they arrive
     */
    Flux<String> generateStream(String systemPrompt, List<Message> history);
}
