package com.agentflow.dto;

import java.util.List;

/**
 * Request body for OpenAI-compatible chat completions API.
 */
public record OpenAiChatRequest(
    String model,
    List<Message> messages,
    Double temperature,
    Integer max_tokens,
    List<String> stop,
    Boolean stream
) {}
