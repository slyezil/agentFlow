package com.agentflow.services;

import com.agentflow.dto.LlamaCompletionRequest;
import com.agentflow.dto.LlamaCompletionResponse;
import com.agentflow.dto.Message;
import com.agentflow.interfaces.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class LlamaCppClient implements LlmClient {

        private static final Logger logger = LoggerFactory.getLogger(LlamaCppClient.class);

        private final WebClient webClient;
        private final int maxTokens;
        private final long timeoutMs;
        private final int maxRetries;

        public LlamaCppClient(
                        @Value("${llama.base-url:http://localhost:8080}") String baseUrl,
                        @Value("${llama.max-tokens:256}") int maxTokens,
                        @Value("${llama.timeout-ms:30000}") long timeoutMs,
                        @Value("${llama.max-retries:3}") int maxRetries) {
                this.webClient = WebClient.builder()
                                .baseUrl(baseUrl)
                                .build();
                this.maxTokens = maxTokens;
                this.timeoutMs = timeoutMs;
                this.maxRetries = maxRetries;
        }

        @Override
        public String generate(String prompt) {
                logger.info("Generating response for prompt: {}", prompt);

                LlamaCompletionRequest request = new LlamaCompletionRequest(
                                prompt,
                                maxTokens,
                                0.2, // low temperature for reasoning
                                new String[] {} // stop tokens later
                );

                String response = webClient.post()
                                .uri("/completion")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(LlamaCompletionResponse.class)
                                .map(LlamaCompletionResponse::content)
                                .timeout(Duration.ofMillis(timeoutMs))
                                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500)))
                                .block();

                logger.info("Generated response: {}", response);
                return response;
        }

        @Override
        public String generate(String systemPrompt, List<Message> history) {
                String formattedPrompt = formatConversation(systemPrompt, history);
                logger.info("Generating response with conversation history ({} messages)", history.size());
                return generate(formattedPrompt);
        }

        /**
         * Formats a conversation history into a single prompt string.
         * Uses a chat-style format compatible with most LLM models.
         */
        private String formatConversation(String systemPrompt, List<Message> history) {
                StringBuilder sb = new StringBuilder();

                // Add system prompt if present
                if (systemPrompt != null && !systemPrompt.isBlank()) {
                        sb.append("System: ").append(systemPrompt).append("\n\n");
                }

                // Add conversation history
                for (Message message : history) {
                        String roleLabel = switch (message.role().toLowerCase()) {
                                case "user" -> "User";
                                case "assistant" -> "Assistant";
                                case "system" -> "System";
                                default -> message.role();
                        };
                        sb.append(roleLabel).append(": ").append(message.content()).append("\n\n");
                }

                // Add prompt for assistant to respond
                sb.append("Assistant:");

                return sb.toString();
        }
}
