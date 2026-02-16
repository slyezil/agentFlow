package com.agentflow.services;

import com.agentflow.dto.Message;
import com.agentflow.interfaces.LlmClient;
import com.agentflow.memory.Conversation;
import com.agentflow.memory.ConversationMemory;
import com.agentflow.memory.MemoryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;
    private final ConversationMemory conversationMemory;
    private final UserPreferenceService userPreferenceService;
    private final MemoryProcessor memoryProcessor;

    public ChatService(LlmClient llmClient,
                       ConversationMemory conversationMemory,
                       UserPreferenceService userPreferenceService,
                       MemoryProcessor memoryProcessor) {
        this.llmClient = llmClient;
        this.conversationMemory = conversationMemory;
        this.userPreferenceService = userPreferenceService;
        this.memoryProcessor = memoryProcessor;
    }

    public String startConversation(String systemPrompt) {
        return conversationMemory.startConversation(systemPrompt);
    }

    public com.agentflow.dto.ChatResponse chat(String conversationId, String userMessageText) {
        Conversation conversation = getConversationOrThrow(conversationId);

        // Extract preferences
        userPreferenceService.extractPreferences(userMessageText);

        // Add user message
        conversationMemory.addMessage(conversationId, new Message("user", userMessageText));

        // Process history
        List<Message> history = conversationMemory.getHistory(conversationId);
        List<Message> processedHistory = memoryProcessor.process(history);

        // Generate response
        String response = llmClient.generate(conversation.getSystemPrompt(), processedHistory);

        // Add assistant response
        conversationMemory.addMessage(conversationId, new Message("assistant", response));

        return new com.agentflow.dto.ChatResponse(conversationId, response, conversationMemory.getHistory(conversationId));
    }

    public Flux<String> chatStream(String conversationId, String userMessageText) {
        Conversation conversation = getConversationOrThrow(conversationId);

        userPreferenceService.extractPreferences(userMessageText);
        conversationMemory.addMessage(conversationId, new Message("user", userMessageText));

        List<Message> history = conversationMemory.getHistory(conversationId);
        List<Message> processedHistory = memoryProcessor.process(history);

        StringBuilder fullResponse = new StringBuilder();

        return llmClient.generateStream(conversation.getSystemPrompt(), processedHistory)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    String completeResponse = fullResponse.toString().trim();
                    if (!completeResponse.isEmpty()) {
                        conversationMemory.addMessage(conversationId, new Message("assistant", completeResponse));
                    }
                });
    }

    private Conversation getConversationOrThrow(String conversationId) {
        return conversationMemory.getConversation(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }
}
