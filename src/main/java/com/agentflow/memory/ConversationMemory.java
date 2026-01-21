package com.agentflow.memory;

import com.agentflow.dto.Message;

import java.util.List;
import java.util.Optional;

public interface ConversationMemory {

    /**
     * Creates a new conversation with an optional system prompt.
     * 
     * @param systemPrompt The system prompt to set for this conversation (can be
     *                     null)
     * @return The unique conversation ID
     */
    String startConversation(String systemPrompt);

    /**
     * Adds a message to an existing conversation.
     * 
     * @param conversationId The conversation to add the message to
     * @param message        The message to add
     * @throws IllegalArgumentException if conversation doesn't exist
     */
    void addMessage(String conversationId, Message message);

    /**
     * Retrieves the full conversation by ID.
     * 
     * @param conversationId The conversation ID
     * @return The conversation if it exists
     */
    Optional<Conversation> getConversation(String conversationId);

    /**
     * Gets the message history for a conversation.
     * 
     * @param conversationId The conversation ID
     * @return List of messages in chronological order
     */
    List<Message> getHistory(String conversationId);

    /**
     * Clears all messages from a conversation but keeps it.
     * 
     * @param conversationId The conversation ID
     */
    void clearConversation(String conversationId);

    /**
     * Deletes a conversation entirely.
     * 
     * @param conversationId The conversation ID
     */
    void deleteConversation(String conversationId);

    /**
     * Lists all conversation IDs.
     * 
     * @return List of all conversation IDs
     */
    List<String> listConversations();
}
