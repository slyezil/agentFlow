package com.agentflow.dto;

public record LlamaCompletionRequest(
        String prompt,
        int n_predict,
        double temperature,
        String[] stop
) {}
