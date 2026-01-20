package com.agentflow.services;

import com.agentflow.dto.LlamaCompletionRequest;
import com.agentflow.dto.LlamaCompletionResponse;
import com.agentflow.interfaces.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                0.2,          // low temperature for reasoning
                new String[]{} // stop tokens later
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
}
