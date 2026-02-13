package com.agentflow.services;

import com.agentflow.dto.*;
import com.agentflow.interfaces.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class LlamaCppClient implements LlmClient {

	private static final Logger logger = LoggerFactory.getLogger(LlamaCppClient.class);

	private final WebClient webClient;
	private final UserPreferenceService userPreferenceService;
	private final int maxTokens;
	private final long timeoutMs;
	private final int maxRetries;

	public LlamaCppClient(
			@Value("${llama.base-url:http://localhost:8080}") String baseUrl,
			@Value("${llama.max-tokens:256}") int maxTokens,
			@Value("${llama.timeout-ms:30000}") long timeoutMs,
			@Value("${llama.max-retries:3}") int maxRetries,
			UserPreferenceService userPreferenceService) {
		this.webClient = WebClient.builder()
				.baseUrl(baseUrl)
				.build();
		this.userPreferenceService = userPreferenceService;
		this.maxTokens = maxTokens;
		this.timeoutMs = timeoutMs;
		this.maxRetries = maxRetries;
	}

	@Override
	public String generate(String prompt) {
		logger.info("Generating response for single prompt (stateless)");
		List<Message> messages = new ArrayList<>();
		
		// Add preferences even in stateless mode if they exist
		String preferencesPrompt = userPreferenceService.getPreferencesPrompt();
		if (!preferencesPrompt.isEmpty()) {
			messages.add(new Message("system", preferencesPrompt));
		}
		
		messages.add(new Message("user", prompt));
		return executeRequest(messages);
	}

	@Override
	public String generate(String systemPrompt, List<Message> history) {
		logger.info("Generating response with conversation history ({} messages)", history.size());

		List<Message> messages = new ArrayList<>();
		
		// Combine system prompt and user preferences
		String preferencesPrompt = userPreferenceService.getPreferencesPrompt();
		StringBuilder fullSystemPrompt = new StringBuilder();
		
		if (systemPrompt != null && !systemPrompt.isBlank()) {
			fullSystemPrompt.append(systemPrompt);
		}
		
		if (!preferencesPrompt.isEmpty()) {
			if (fullSystemPrompt.length() > 0) {
				fullSystemPrompt.append("\n\n");
			}
			fullSystemPrompt.append(preferencesPrompt);
		}

		if (fullSystemPrompt.length() > 0) {
			messages.add(new Message("system", fullSystemPrompt.toString()));
		}
		
		messages.addAll(history);

		return executeRequest(messages);
	}

	private String executeRequest(List<Message> messages) {
		OpenAiChatRequest request = new OpenAiChatRequest(
				"default", 
				messages,
				0.2,
				maxTokens,
				List.of("###", "\nUser:", "\nAssistant:"),
				false);

		OpenAiChatResponse response = webClient.post()
				.uri("/v1/chat/completions")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(OpenAiChatResponse.class)
				.timeout(Duration.ofMillis(timeoutMs))
				.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500)))
				.block();

		if (response != null && !response.choices().isEmpty()) {
			String content = response.choices().get(0).message().content();
			if (content != null) {
				content = content.trim();
				logger.info("Generated response: {}", content);
				return content;
			}
		}

		logger.warn("Received empty or null response from LLM server");
		return "";
	}
}
