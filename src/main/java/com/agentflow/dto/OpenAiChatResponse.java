package com.agentflow.dto;

import java.util.List;

/**
 * Response body for OpenAI-compatible chat completions API.
 */
public record OpenAiChatResponse(
    String id,
    String object,
    Long created,
    String model,
    List<Choice> choices
) {
    public record Choice(
        Message message,
        String finish_reason,
        Integer index
    ) {}
}
