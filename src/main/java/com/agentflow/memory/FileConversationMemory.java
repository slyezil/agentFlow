package com.agentflow.memory;

import com.agentflow.dto.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A file-backed implementation of ConversationMemory.
 * Each conversation is stored as a JSON file in the configured data directory.
 * Conversations persist across server restarts.
 */
public class FileConversationMemory implements ConversationMemory {

    private static final Logger logger = LoggerFactory.getLogger(FileConversationMemory.class);

    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Path dataDir;
    private final ObjectMapper objectMapper;

    public FileConversationMemory(String dataDirPath) {
        this.dataDir = Paths.get(dataDirPath, "conversations");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        initializeDataDir();
        loadExistingConversations();
    }

    private void initializeDataDir() {
        try {
            Files.createDirectories(dataDir);
            logger.info("FileConversationMemory initialized at: {}", dataDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create data directory: {}", dataDir, e);
            throw new RuntimeException("Cannot initialize file-based memory", e);
        }
    }

    private void loadExistingConversations() {
        try (Stream<Path> files = Files.list(dataDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(this::loadConversation);
            logger.info("Loaded {} existing conversations from disk", conversations.size());
        } catch (IOException e) {
            logger.warn("Could not load existing conversations: {}", e.getMessage());
        }
    }

    private void loadConversation(Path file) {
        try {
            Map<String, Object> data = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            String id = (String) data.get("id");
            String systemPrompt = (String) data.get("systemPrompt");
            List<Map<String, String>> rawMessages = (List<Map<String, String>>) data.get("messages");

            Conversation conversation = new Conversation(id, systemPrompt);
            if (rawMessages != null) {
                for (Map<String, String> msg : rawMessages) {
                    conversation.addMessage(new Message(msg.get("role"), msg.get("content")));
                }
            }
            conversations.put(id, conversation);
        } catch (IOException e) {
            logger.warn("Failed to load conversation from {}: {}", file, e.getMessage());
        }
    }

    private void saveConversation(Conversation conversation) {
        Path file = dataDir.resolve(conversation.getId() + ".json");
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", conversation.getId());
            data.put("systemPrompt", conversation.getSystemPrompt());
            data.put("createdAt", conversation.getCreatedAt().toString());
            data.put("updatedAt", conversation.getUpdatedAt().toString());

            List<Map<String, String>> messages = new ArrayList<>();
            for (Message msg : conversation.getMessages()) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
            data.put("messages", messages);

            objectMapper.writeValue(file.toFile(), data);
        } catch (IOException e) {
            logger.error("Failed to save conversation {}: {}", conversation.getId(), e.getMessage());
        }
    }

    @Override
    public String startConversation(String systemPrompt) {
        String conversationId = UUID.randomUUID().toString();
        Conversation conversation = new Conversation(conversationId, systemPrompt);
        conversations.put(conversationId, conversation);
        saveConversation(conversation);
        logger.info("Started new conversation (file-backed): {}", conversationId);
        return conversationId;
    }

    @Override
    public void addMessage(String conversationId, Message message) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        conversation.addMessage(message);
        saveConversation(conversation);
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
            saveConversation(conversation);
            logger.info("Cleared conversation: {}", conversationId);
        }
    }

    @Override
    public void deleteConversation(String conversationId) {
        Conversation removed = conversations.remove(conversationId);
        if (removed != null) {
            Path file = dataDir.resolve(conversationId + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                logger.warn("Failed to delete conversation file: {}", file);
            }
            logger.info("Deleted conversation: {}", conversationId);
        }
    }

    @Override
    public List<String> listConversations() {
        return new ArrayList<>(conversations.keySet());
    }
}
