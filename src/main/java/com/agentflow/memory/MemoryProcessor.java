package com.agentflow.memory;

import com.agentflow.dto.Message;
import java.util.List;

/**
 * Interface for processing conversation history before sending it to the LLM.
 * Implementations can trim, summarize, or otherwise modify the message list
 * to fit within context windows.
 */
public interface MemoryProcessor {
    List<Message> process(List<Message> history);
}
