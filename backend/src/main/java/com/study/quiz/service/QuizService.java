package com.study.quiz.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.study.quiz.dto.QuizItem;
import com.study.quiz.dto.QuizRequest;
import com.study.quiz.dto.QuizResponse;
import com.study.quiz.dto.QuizSessionSummary;
import com.study.quiz.entity.QuizItemEntity;
import com.study.quiz.entity.QuizSession;
import com.study.quiz.repository.QuizItemRepository;
import com.study.quiz.repository.QuizSessionRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * OpenAI Chat Completions(JSON)을 WebClient로 호출한다.
 * 프롬프트·파싱을 한 곳에 모아 컨트롤러는 입출력 DTO만 다루게 한다.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

	private static final int MAX_QUESTIONS = 20;
	private static final int DEFAULT_COUNT = 5;

	private static final String SYSTEM_MESSAGE = """
			Return only JSON that matches the given json_schema. No prose, no markdown, no code fences (no ``` or ```json).

			알고리즘 관련 문제일 경우, 반드시 해당 코드의 시간 복잡도(Big-O)와 공간 복잡도 설명을 complexityAnalysis 필드에 포함하라.
			When the item is not algorithm- or asymptotic-analysis-related, fill complexityAnalysis with one short Korean sentence that states it is not applicable.

			For detailedExplanation: include why the correct option is right and why each wrong option is mistaken (reasoning, traps, complexity, or misread code). Write in Korean unless a code identifier must stay in English.
			Structure detailedExplanation as two segments separated by a single line containing exactly "---WRONG-ANALYSIS---" (no spaces): before that line, only the correct-answer rationale; after that line, only the wrong-option analysis (reference options by number 1–4 or by paraphrase).
			""";

	private final WebClient openAiWebClient;
	private final JsonMapper jsonMapper;
	private final QuizSessionRepository sessionRepository;
	private final QuizItemRepository itemRepository;

	@Value("${openai.api-key}")
	private String apiKey;

	@Value("${openai.model}")
	private String model;

	public Mono<QuizResponse> generateQuiz(QuizRequest request) {
		int count = clampCount(request.getQuestionCount());
		String topic = blankToDefault(request.getTopic(), "IT / Computer Science");
		String difficulty = blankToDefault(request.getDifficulty(), "medium");
		String studyContent = request.getContent() == null ? "" : request.getContent();
		String userPrompt = buildPrompt(topic, difficulty, count, studyContent);

		Map<String, Object> jsonSchemaWrapper = new LinkedHashMap<>();
		jsonSchemaWrapper.put("name", "quiz_response");
		jsonSchemaWrapper.put("strict", Boolean.TRUE);
		jsonSchemaWrapper.put("schema", openAiQuizRootSchema());

		Map<String, Object> responseFormat = new LinkedHashMap<>();
		responseFormat.put("type", "json_schema");
		responseFormat.put("json_schema", jsonSchemaWrapper);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("temperature", 0.45);
		body.put("max_tokens", maxTokensForCount(count));
		body.put("response_format", responseFormat);
		body.put("messages", List.of(
				Map.of("role", "system", "content", SYSTEM_MESSAGE.trim()),
				Map.of("role", "user", "content", userPrompt)));

		return openAiWebClient.post()
				.uri("/v1/chat/completions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(this::extractQuizJson)
				.map(this::parseItems)
				.flatMap(items -> persistSession(request, items).thenReturn(items))
				.map(QuizResponse::new)
				.onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
						"OpenAI API 오류: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(), ex));
	}

	public List<QuizSessionSummary> getHistory() {
		return sessionRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(QuizSessionSummary::new)
				.toList();
	}

	public QuizResponse getHistoryDetail(Long sessionId) {
		List<QuizItemEntity> entities = itemRepository.findBySessionId(sessionId);
		List<QuizItem> items = entities.stream().map(this::toQuizItem).toList();
		return new QuizResponse(items);
	}

	private Mono<Void> persistSession(QuizRequest request, List<QuizItem> items) {
		return Mono.fromRunnable(() -> {
			QuizSession session = sessionRepository.save(QuizSession.builder()
					.topic(request.getTopic())
					.difficulty(request.getDifficulty())
					.questionCount(items.size())
					.score(0)
					.totalCount(items.size())
					.createdAt(LocalDateTime.now())
					.build());

			List<QuizItemEntity> entities = new ArrayList<>();
			for (QuizItem item : items) {
				entities.add(QuizItemEntity.builder()
						.session(session)
						.question(item.getQuestion())
						.optionsJson(serializeOptions(item.getOptions()))
						.correctIndex(item.getCorrectIndex())
						.explanation(item.getExplanation())
						.concept(item.getRelatedConcept())
						.build());
			}
			itemRepository.saveAll(entities);
		}).subscribeOn(Schedulers.boundedElastic()).then();
	}

	private String serializeOptions(List<String> options) {
		try {
			return jsonMapper.writeValueAsString(options);
		} catch (Exception e) {
			throw new IllegalStateException("options 직렬화 실패", e);
		}
	}

	private QuizItem toQuizItem(QuizItemEntity entity) {
		List<String> options;
		try {
			options = jsonMapper.readValue(entity.getOptionsJson(), new TypeReference<List<String>>() {});
		} catch (Exception e) {
			options = List.of();
		}
		return QuizItem.builder()
				.question(entity.getQuestion())
				.options(options)
				.correctIndex(entity.getCorrectIndex())
				.explanation(entity.getExplanation())
				.relatedConcept(entity.getConcept())
				.build();
	}

	/** OpenAI Structured Outputs용 JSON Schema(표준 type 소문자). */
	private static Map<String, Object> openAiQuizItemSchema() {
		Map<String, Object> optionsArr = new LinkedHashMap<>();
		optionsArr.put("type", "array");
		optionsArr.put("items", Map.of("type", "string"));
		optionsArr.put("minItems", 4);
		optionsArr.put("maxItems", 4);

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("question", Map.of("type", "string"));
		properties.put("options", optionsArr);
		properties.put("correctIndex", Map.of(
				"type", "integer",
				"minimum", 0,
				"maximum", 3));
		properties.put("explanation", Map.of("type", "string"));
		properties.put("detailedExplanation", Map.of("type", "string"));
		properties.put("relatedConcept", Map.of("type", "string"));
		properties.put("complexityAnalysis", Map.of("type", "string"));

		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", List.of(
				"question", "options", "correctIndex", "explanation", "detailedExplanation", "relatedConcept",
				"complexityAnalysis"));
		schema.put("additionalProperties", Boolean.FALSE);
		return schema;
	}

	private static Map<String, Object> openAiQuizRootSchema() {
		Map<String, Object> items = new LinkedHashMap<>();
		items.put("type", "array");
		items.put("items", openAiQuizItemSchema());

		Map<String, Object> properties = Map.of("items", items);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "object");
		root.put("properties", properties);
		root.put("required", List.of("items"));
		root.put("additionalProperties", Boolean.FALSE);
		return root;
	}

	private int clampCount(int raw) {
		if (raw < 1) {
			return DEFAULT_COUNT;
		}
		return Math.min(raw, MAX_QUESTIONS);
	}

	/**
	 * 문항 수에 비례해 출력 상한을 둔다. 대략 3문항 기준 1000~1500 토큰대가 되도록 스케일한다.
	 */
	private static int maxTokensForCount(int questionCount) {
		int scaled = 350 + questionCount * 520;
		return Math.min(8192, Math.max(1024, scaled));
	}

	private static String blankToDefault(String value, String def) {
		if (value == null || value.isBlank()) {
			return def;
		}
		return value.trim();
	}

	private String buildPrompt(String topic, String difficulty, int count, String studyContent) {
		String notes = studyContent.isBlank()
				? "Notes: none; scope from topic only."
				: "Ground ≥half of items in notes (paraphrase). Notes:\n---\n%s\n---".formatted(studyContent.trim());

		return """
				Return only raw JSON matching the response schema (one object with "items"). No other text.
				Do not use markdown code fences of any kind (no ```, no ```json, no closing fences).

				MCQ. Topic: %s. Difficulty tone: %s. %s

				Exactly %d items. Each: 4 distinct string options; correctIndex 0–3; explanation 2–4 sentences Korean (English terms OK); relatedConcept one short label; complexityAnalysis per system rules (Big-O time and space when algorithmic, else one short N/A sentence).
				detailedExplanation: longer than explanation; must use the exact line "---WRONG-ANALYSIS---" between correct-answer rationale (before) and wrong-option analysis (after), per system message.
				CS focus: code behavior, complexity, DS tradeoffs, pitfalls. Vary subtopics across items.
				""".formatted(topic, difficulty, notes, count);
	}

	private String extractQuizJson(JsonNode root) {
		JsonNode choices = root.path("choices");
		if (!choices.isArray() || choices.isEmpty()) {
			throw new IllegalStateException("OpenAI 응답에 choices가 없습니다.");
		}
		String content = choices.get(0).path("message").path("content").asText("");
		if (content.isBlank()) {
			throw new IllegalStateException("OpenAI 응답 message.content가 비었습니다.");
		}
		return stripMarkdownFence(content);
	}

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
			JsonNode root = jsonMapper.readTree(json);
			JsonNode arr = root.path("items").isArray()
					? root.path("items")
					: root.isArray() ? root : null;
			if (arr == null || !arr.isArray()) {
				throw new IllegalStateException("응답 JSON에 items 배열(또는 루트 배열)이 없습니다.");
			}
			return jsonMapper.convertValue(arr, new TypeReference<List<QuizItem>>() {
			});
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("퀴즈 JSON 파싱 실패: " + e.getMessage(), e);
		}
	}
}
