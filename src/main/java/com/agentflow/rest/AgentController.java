package com.agentflow.rest;

import com.agentflow.interfaces.LlmClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final LlmClient llmClient;

    public AgentController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String response = llmClient.generate(prompt);
        return Map.of("response", response);
    }
}
