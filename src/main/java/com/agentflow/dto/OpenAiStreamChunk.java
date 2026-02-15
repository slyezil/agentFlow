package com.agentflow.dto;

import java.util.List;

/**
 * Response body for OpenAI-compatible streaming chat completions.
 * In streaming mode, each chunk contains a 'delta' (partial content)
 * instead of a full 'message'.
 */
public record OpenAiStreamChunk(
    String id,
    String object,
    Long created,
    String model,
    List<Choice> choices
) {
    public record Choice(
        Delta delta,
        String finish_reason,
        Integer index
    ) {}

    public record Delta(
        String role,
        String content
    ) {}
}
