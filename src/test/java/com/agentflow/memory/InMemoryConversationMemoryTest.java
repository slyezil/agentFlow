package com.agentflow.memory;

import com.agentflow.dto.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryConversationMemoryTest {

    private InMemoryConversationMemory memory;

    @BeforeEach
    void setUp() {
        memory = new InMemoryConversationMemory();
    }

    @Test
    void testStartConversation() {
        String systemPrompt = "Test system prompt";
        String id = memory.startConversation(systemPrompt);

        assertNotNull(id);
        assertFalse(id.isBlank());

        Optional<Conversation> conversation = memory.getConversation(id);
        assertTrue(conversation.isPresent());
        assertEquals(systemPrompt, conversation.get().getSystemPrompt());
    }

    @Test
    void testAddAndGetHistory() {
        String id = memory.startConversation(null);
        Message message1 = new Message("user", "Hello");
        Message message2 = new Message("assistant", "Hi there!");

        memory.addMessage(id, message1);
        memory.addMessage(id, message2);

        List<Message> history = memory.getHistory(id);
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).role());
        assertEquals("Hello", history.get(0).content());
        assertEquals("assistant", history.get(1).role());
    }

    @Test
    void testClearConversation() {
        String id = memory.startConversation(null);
        memory.addMessage(id, new Message("user", "Hello"));

        memory.clearConversation(id);

        List<Message> history = memory.getHistory(id);
        assertTrue(history.isEmpty());
        assertTrue(memory.getConversation(id).isPresent(), "Conversation metadata should still exist");
    }

    @Test
    void testDeleteConversation() {
        String id = memory.startConversation(null);
        memory.deleteConversation(id);

        assertFalse(memory.getConversation(id).isPresent());
        assertEquals(0, memory.listConversations().size());
    }

    @Test
    void testListConversations() {
        memory.startConversation(null);
        memory.startConversation(null);

        List<String> ids = memory.listConversations();
        assertEquals(2, ids.size());
    }

    @Test
    void testMultiTurnMemoryConsistency() {
        String id = memory.startConversation("System prompt");

        // Turn 1
        memory.addMessage(id, new Message("user", "My name is Alex"));
        memory.addMessage(id, new Message("assistant", "Hello Alex!"));

        // Turn 2
        memory.addMessage(id, new Message("user", "Where am I?"));
        memory.addMessage(id, new Message("assistant", "You are in a test environment."));

        List<Message> history = memory.getHistory(id);
        assertEquals(4, history.size());
        assertEquals("user", history.get(0).role());
        assertEquals("Alex", history.get(0).content().split("is ")[1]); // Simple verification
        assertEquals("assistant", history.get(3).role());
        assertTrue(history.get(3).content().contains("test environment"));
    }

    @Test
    void testAddMessageToNonExistentConversation() {
        assertThrows(IllegalArgumentException.class, () -> {
            memory.addMessage("invalid-id", new Message("user", "test"));
        });
    }
}
