package com.agentflow.memory;

import com.agentflow.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryConversationMemory implements ConversationMemory {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryConversationMemory.class);

    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public String startConversation(String systemPrompt) {
        String conversationId = UUID.randomUUID().toString();
        Conversation conversation = new Conversation(conversationId, systemPrompt);
        conversations.put(conversationId, conversation);
        logger.info("Started new conversation: {}", conversationId);
        return conversationId;
    }

    @Override
    public void addMessage(String conversationId, Message message) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        conversation.addMessage(message);
        logger.debug("Added message to conversation {}: role={}", conversationId, message.role());
    }

    @Override
    public Optional<Conversation> getConversation(String conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    @Override
    public List<Message> getHistory(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            return Collections.emptyList();
        }
        return conversation.getMessages();
    }

    @Override
    public void clearConversation(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.clear();
            logger.info("Cleared conversation: {}", conversationId);
        }
    }

    @Override
    public void deleteConversation(String conversationId) {
        Conversation removed = conversations.remove(conversationId);
        if (removed != null) {
            logger.info("Deleted conversation: {}", conversationId);
        }
    }

    @Override
    public List<String> listConversations() {
        return new ArrayList<>(conversations.keySet());
    }
}
