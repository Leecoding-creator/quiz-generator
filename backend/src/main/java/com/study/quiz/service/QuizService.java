package com.study.quiz.service;

import java.util.LinkedHashMap;
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
		String studyContent = request.getContent() == null ? "" : request.getContent();
		String prompt = buildPrompt(topic, difficulty, count, studyContent);

		// responseMimeType + responseSchema를 같이 쓰면 모델이 마크다운으로 빠지지 않고 JSON 배열만보낸다.
		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("temperature", 0.45);
		generationConfig.put("responseSchema", quizResponseObjectSchema());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(
				Map.of("parts", List.of(Map.of("text", prompt)))));
		body.put("generationConfig", generationConfig);

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

	/**
	 * 루트를 OBJECT로 두면 Gemini structured JSON 규약과 잘 맞고, MIME이 application/json인 객체 한 덩어리로 고정된다.
	 */
	private static Map<String, Object> quizResponseObjectSchema() {
		Map<String, Object> itemProps = new LinkedHashMap<>();
		itemProps.put("question", Map.of("type", "STRING"));
		itemProps.put("options", Map.of(
				"type", "ARRAY",
				"items", Map.of("type", "STRING")));
		itemProps.put("correctIndex", Map.of("type", "INTEGER"));
		itemProps.put("explanation", Map.of("type", "STRING"));
		itemProps.put("relatedConcept", Map.of("type", "STRING"));

		Map<String, Object> itemSchema = new LinkedHashMap<>();
		itemSchema.put("type", "OBJECT");
		itemSchema.put("properties", itemProps);
		itemSchema.put("required", List.of(
				"question", "options", "correctIndex", "explanation", "relatedConcept"));

		Map<String, Object> listSchema = new LinkedHashMap<>();
		listSchema.put("type", "ARRAY");
		listSchema.put("items", itemSchema);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "OBJECT");
		root.put("properties", Map.of("items", listSchema));
		root.put("required", List.of("items"));
		return root;
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

	private String buildPrompt(String topic, String difficulty, int count, String studyContent) {
		String groundedBlock = studyContent.isBlank()
				? "Learner notes: (none). Scope the questions strictly from the topic; still use short code or pseudocode where it helps assess reading skill."
				: """
						Ground at least half of the questions in the learner notes below (paraphrase code, ask about behavior, complexity, or refactor impact). If notes are incomplete, fill gaps only from standard CS knowledge consistent with the topic.
						Learner notes:
						---
						%s
						---
						""".formatted(studyContent.trim());

		return """
				You are an expert CS coach building a multiple-choice drill for practical interview / lab exam readiness.

				Topic: "%s"
				Declared difficulty tone: %s
				%s

				Focus (CS 실기 역량): prioritize (1) reading and predicting behavior of short Java-like or pseudocode, (2) time/space complexity and scalability, (3) data structure / API choice and trade-offs, (4) memory, concurrency, or I/O efficiency pitfalls, (5) small refactor or bug-spotting that changes Big-O or correctness.

				Rules:
				- Produce exactly %d items.
				- Each item: exactly 4 distinct options (strings), one clearly best answer.
				- Vary sub-skills across items; avoid repeating the same pattern every time.
				- explanation: 2–4 sentences tying the correct option to evidence from code or complexity reasoning; mention why the strongest distractor is wrong when useful.
				- relatedConcept: one concise CS concept label (Korean or English, be consistent within the set).

				Output must match the response schema exactly: one JSON object with an "items" array (no markdown, no prose outside JSON).
				""".formatted(topic, difficulty, groundedBlock, count);
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

	/** 스키마 모드에서도 방어적으로 펜스가 붙은 경우를 제거해 파서가 깨지지 않게 한다. */
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
			JsonNode root = objectMapper.readTree(json);
			JsonNode arr = root.path("items").isArray()
					? root.path("items")
					: root.isArray() ? root : null;
			if (arr == null || !arr.isArray()) {
				throw new IllegalStateException("응답 JSON에 items 배열(또는 루트 배열)이 없습니다.");
			}
			return objectMapper.convertValue(arr, new TypeReference<List<QuizItem>>() {
			});
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("퀴즈 JSON 파싱 실패: " + e.getMessage(), e);
		}
	}
}
