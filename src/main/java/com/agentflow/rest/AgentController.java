package com.agentflow.rest;

import com.agentflow.dto.*;
import com.agentflow.interfaces.LlmClient;
import com.agentflow.memory.Conversation;
import com.agentflow.memory.ConversationMemory;
import com.agentflow.services.UserPreferenceService;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final LlmClient llmClient;
    private final ConversationMemory conversationMemory;
    private final UserPreferenceService userPreferenceService;

    public AgentController(LlmClient llmClient, ConversationMemory conversationMemory, UserPreferenceService userPreferenceService) {
        this.llmClient = llmClient;
        this.conversationMemory = conversationMemory;
        this.userPreferenceService = userPreferenceService;
    }

    // ==================== Backward Compatible Endpoint ====================

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String response = llmClient.generate(prompt);
        return Map.of("response", response);
    }

    // ==================== Conversation Endpoints ====================

    @PostMapping("/conversations")
    public CreateConversationResponse createConversation(
            @RequestBody(required = false) CreateConversationRequest request) {
        String systemPrompt = (request != null) ? request.systemPrompt() : null;
        String conversationId = conversationMemory.startConversation(systemPrompt);
        return new CreateConversationResponse(conversationId);
    }

    @GetMapping("/conversations")
    public List<String> listConversations() {
        return conversationMemory.listConversations();
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> getConversation(@PathVariable("id") String conversationId) {
        Conversation conversation = conversationMemory.getConversation(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        return Map.of(
                "conversationId", conversation.getId(),
                "systemPrompt", conversation.getSystemPrompt() != null ? conversation.getSystemPrompt() : "",
                "messages", conversation.getMessages(),
                "createdAt", conversation.getCreatedAt().toString(),
                "updatedAt", conversation.getUpdatedAt().toString());
    }

    @PostMapping("/conversations/{id}/chat")
    public ChatResponse chat(@PathVariable("id") String conversationId, @RequestBody ChatRequest request) {
        Conversation conversation = conversationMemory.getConversation(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        // Extract user preferences
        userPreferenceService.extractPreferences(request.message());

        // Add user message to history
        Message userMessage = new Message("user", request.message());
        conversationMemory.addMessage(conversationId, userMessage);

        // Generate response with full context
        List<Message> history = conversationMemory.getHistory(conversationId);
        String response = llmClient.generate(conversation.getSystemPrompt(), history);

        // Add assistant response to history
        Message assistantMessage = new Message("assistant", response);
        conversationMemory.addMessage(conversationId, assistantMessage);

        // Return updated history
        List<Message> updatedHistory = conversationMemory.getHistory(conversationId);
        return new ChatResponse(conversationId, response, updatedHistory);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable("id") String conversationId) {
        conversationMemory.deleteConversation(conversationId);
    }
}
