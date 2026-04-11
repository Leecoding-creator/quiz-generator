package com.study.quiz.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.quiz.dto.QuizItem;
import com.study.quiz.dto.QuizRequest;
import com.study.quiz.dto.QuizResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Gemini는 HTTP JSON API이므로 WebClient로 호출해 스레드를 블로킹하지 않는다.
 * 프롬프트·파싱을 한 곳에 모아 컨트롤러는 입출력 DTO만 다루게 한다.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

	private static final int MAX_QUESTIONS = 20;
	private static final int DEFAULT_COUNT = 5;

	private final WebClient geminiWebClient;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model}")
	private String model;

	public Mono<QuizResponse> generateQuiz(QuizRequest request) {
		int count = clampCount(request.getQuestionCount());
		String topic = blankToDefault(request.getTopic(), "IT / Computer Science");
		String difficulty = blankToDefault(request.getDifficulty(), "medium");
		String prompt = buildPrompt(topic, difficulty, count);

		// responseMimeType을 JSON으로 두면 후처리가 단순해지고 잘린 마크다운 설명이 줄어든다.
		Map<String, Object> body = Map.of(
				"contents", List.of(
						Map.of("parts", List.of(Map.of("text", prompt)))),
				"generationConfig", Map.of(
						"responseMimeType", "application/json",
						"temperature", 0.6));

		return geminiWebClient.post()
				.uri(uriBuilder -> uriBuilder
						.path("/v1beta/models/{model}:generateContent")
						.queryParam("key", apiKey)
						.build(model))
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(this::extractQuizJson)
				.map(this::parseItems)
				.map(QuizResponse::new)
				.onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
						"Gemini API 오류: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(), ex));
	}

	private int clampCount(int raw) {
		if (raw < 1) {
			return DEFAULT_COUNT;
		}
		return Math.min(raw, MAX_QUESTIONS);
	}

	private static String blankToDefault(String value, String def) {
		if (value == null || value.isBlank()) {
			return def;
		}
		return value.trim();
	}

	private String buildPrompt(String topic, String difficulty, int count) {
		return """
				You are a quiz generator. Create exactly %d multiple-choice questions about: "%s".
				Difficulty level: %s.
				Each question must have exactly 4 options as strings.
				Respond with a JSON array ONLY (no markdown fences), each element:
				{"question":"...","options":["A","B","C","D"],"correctIndex":0}
				where correctIndex is 0-3 for the correct option.
				""".formatted(count, topic, difficulty);
	}

	private String extractQuizJson(JsonNode root) {
		JsonNode candidates = root.path("candidates");
		if (!candidates.isArray() || candidates.isEmpty()) {
			throw new IllegalStateException("Gemini 응답에 candidates가 없습니다.");
		}
		JsonNode parts = candidates.get(0).path("content").path("parts");
		if (!parts.isArray() || parts.isEmpty()) {
			throw new IllegalStateException("Gemini 응답에 parts가 없습니다.");
		}
		String text = parts.get(0).path("text").asText("");
		if (text.isBlank()) {
			throw new IllegalStateException("Gemini 응답 텍스트가 비었습니다.");
		}
		return stripMarkdownFence(text);
	}

	/** 모델이 지시를 어기고 코드 펜스를 쓸 때를 대비해 순수 JSON만 남긴다. */
	private static String stripMarkdownFence(String text) {
		String t = text.trim();
		if (t.startsWith("```")) {
			int firstNl = t.indexOf('\n');
			int lastFence = t.lastIndexOf("```");
			if (firstNl > 0 && lastFence > firstNl) {
				t = t.substring(firstNl + 1, lastFence).trim();
			}
		}
		return t;
	}

	private List<QuizItem> parseItems(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<List<QuizItem>>() {
			});
		} catch (Exception e) {
			throw new IllegalStateException("퀴즈 JSON 파싱 실패: " + e.getMessage(), e);
		}
	}
}
