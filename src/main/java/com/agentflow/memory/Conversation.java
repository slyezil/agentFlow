package com.agentflow.memory;

import com.agentflow.dto.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Conversation {
    private final String id;
    private final String systemPrompt;
    private final List<Message> messages;
    private final Instant createdAt;
    private volatile Instant updatedAt;

    public Conversation(String id, String systemPrompt) {
        this.id = id;
        this.systemPrompt = systemPrompt;
        this.messages = new CopyOnWriteArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public synchronized void addMessage(Message message) {
        messages.add(message);
        updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public synchronized void clear() {
        messages.clear();
        updatedAt = Instant.now();
    }
}
