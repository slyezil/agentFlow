package com.agentflow.services;

import com.agentflow.dto.*;
import com.agentflow.interfaces.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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

	private final double temperature;
	private final List<String> stopSequences;

	public LlamaCppClient(
			@Value("${llama.base-url:http://localhost:8080}") String baseUrl,
			@Value("${llama.max-tokens:256}") int maxTokens,
			@Value("${llama.timeout-ms:30000}") long timeoutMs,
			@Value("${llama.max-retries:3}") int maxRetries,
			@Value("${llama.temperature:0.7}") double temperature,
			@Value("${llama.stop-sequences:###,\\nUser:,\\nAssistant:}") List<String> stopSequences,
			UserPreferenceService userPreferenceService) {
		this.webClient = WebClient.builder()
				.baseUrl(baseUrl)
				.build();
		this.userPreferenceService = userPreferenceService;
		this.maxTokens = maxTokens;
		this.timeoutMs = timeoutMs;
		this.maxRetries = maxRetries;
		this.temperature = temperature;
		this.stopSequences = stopSequences;
	}

	@Override
	public String generate(String prompt) {
		logger.info("Generating response for single prompt (stateless)");
		List<Message> messages = new ArrayList<>();
		
		String preferencesPrompt = userPreferenceService.getPreferencesPrompt();
		if (!preferencesPrompt.isEmpty()) {
			messages.add(new Message("system", preferencesPrompt));
		}
		
		messages.add(new Message("user", prompt));
		return executeRequest(messages);
	}

	@Override
	public String generateRaw(String prompt) {
		logger.info("Generating raw response (no preferences)");
		List<Message> messages = List.of(new Message("user", prompt));
		return executeRequest(messages);
	}

	@Override
	public String generate(String systemPrompt, List<Message> history) {
		logger.info("Generating response with conversation history ({} messages)", history.size());
		List<Message> messages = buildMessages(systemPrompt, history);
		return executeRequest(messages);
	}

	@Override
	public Flux<String> generateStream(String systemPrompt, List<Message> history) {
		logger.info("Streaming response with conversation history ({} messages)", history.size());
		List<Message> messages = buildMessages(systemPrompt, history);
		return executeStreamRequest(messages);
	}

	private List<Message> buildMessages(String systemPrompt, List<Message> history) {
		List<Message> messages = new ArrayList<>();

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
		return messages;
	}

	private String executeRequest(List<Message> messages) {
		logger.info("Sending {} messages to LLM (timeout={}ms, max-tokens={})", 
				messages.size(), timeoutMs, maxTokens);

		OpenAiChatRequest request = new OpenAiChatRequest(
				"default", 
				messages,
				temperature,
				maxTokens,
				stopSequences,
				false);

		OpenAiChatResponse response = webClient.post()
				.uri("/v1/chat/completions")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(OpenAiChatResponse.class)
				.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500))
						.filter(ex -> !(ex instanceof java.util.concurrent.TimeoutException))
						.doBeforeRetry(signal -> logger.warn("Retrying LLM request (attempt {}): {}",
								signal.totalRetries() + 1, signal.failure().getMessage())))
				.timeout(Duration.ofMillis(timeoutMs))
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

	private Flux<String> executeStreamRequest(List<Message> messages) {
		logger.info("Streaming {} messages to LLM (timeout={}ms, max-tokens={})",
				messages.size(), timeoutMs, maxTokens);

		OpenAiChatRequest request = new OpenAiChatRequest(
				"default",
				messages,
				temperature,
				maxTokens,
				stopSequences,
				true); // stream = true

		return webClient.post()
				.uri("/v1/chat/completions")
				.bodyValue(request)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<OpenAiStreamChunk>>() {})
				.timeout(Duration.ofMillis(timeoutMs))
				.mapNotNull(sse -> {
					OpenAiStreamChunk chunk = sse.data();
					if (chunk != null && chunk.choices() != null && !chunk.choices().isEmpty()) {
						OpenAiStreamChunk.Delta delta = chunk.choices().get(0).delta();
						if (delta != null && delta.content() != null) {
							return delta.content();
						}
					}
					return null;
				})
				.doOnComplete(() -> logger.info("Stream completed"))
				.doOnError(e -> logger.error("Stream error: {}", e.getMessage()));
	}
}
