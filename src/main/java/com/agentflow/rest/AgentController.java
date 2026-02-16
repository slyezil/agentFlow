package com.agentflow.rest;

import com.agentflow.dto.*;
import com.agentflow.interfaces.LlmClient;
import com.agentflow.memory.Conversation;
import com.agentflow.memory.ConversationMemory;
import com.agentflow.services.ChatService;
import com.agentflow.services.UserPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final LlmClient llmClient;
    private final ConversationMemory conversationMemory;
    private final UserPreferenceService userPreferenceService;
    private final ChatService chatService;

    public AgentController(LlmClient llmClient,
                           ConversationMemory conversationMemory,
                           UserPreferenceService userPreferenceService,
                           ChatService chatService) {
        this.llmClient = llmClient;
        this.conversationMemory = conversationMemory;
        this.userPreferenceService = userPreferenceService;
        this.chatService = chatService;
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
        return chatService.chat(conversationId, request.message());
    }

    // ==================== Streaming Chat Endpoint ====================

    @PostMapping(value = "/conversations/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@PathVariable("id") String conversationId, @RequestBody ChatRequest request) {
        return chatService.chatStream(conversationId, request.message());
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable("id") String conversationId) {
        conversationMemory.deleteConversation(conversationId);
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public Map<String, String> health() {
        try {
            // fast connectivity check
            llmClient.generateRaw("ping"); 
            return Map.of("status", "UP", "llm", "CONNECTED");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "llm", "DISCONNECTED", "error", e.getMessage());
        }
    }
}
