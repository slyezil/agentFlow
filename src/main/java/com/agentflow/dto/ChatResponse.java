package com.agentflow.dto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String response,
        List<Message> history) {
}
