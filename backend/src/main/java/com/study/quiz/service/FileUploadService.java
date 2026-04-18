package com.study.quiz.service;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tools.jackson.databind.JsonNode;
import com.study.quiz.dto.UploadResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class FileUploadService {

	private static final int EXTRACT_CHAR_LIMIT = 8000;

	private final WebClient openAiWebClient;

	@Value("${openai.api-key}")
	private String apiKey;

	@Value("${openai.model}")
	private String model;

	public Mono<UploadResponse> processUpload(MultipartFile file) {
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			return Mono.error(new IllegalStateException("파일 읽기 실패: " + e.getMessage(), e));
		}

		String contentType = file.getContentType() != null ? file.getContentType() : "";

		Mono<String> extractedMono;
		if (contentType.equals("application/pdf")) {
			extractedMono = extractFromPdf(bytes);
		} else if (contentType.startsWith("image/")) {
			extractedMono = extractFromImage(bytes, contentType);
		} else {
			return Mono.error(new IllegalArgumentException(
					"지원하지 않는 파일 형식입니다. PDF 또는 이미지(JPG, PNG)를 업로드해주세요."));
		}

		return extractedMono.flatMap(extracted -> {
			String forSummary = extracted.length() > EXTRACT_CHAR_LIMIT
					? extracted.substring(0, EXTRACT_CHAR_LIMIT)
					: extracted;
			return summarize(forSummary).map(summary -> new UploadResponse(extracted, summary));
		});
	}

	private Mono<String> extractFromPdf(byte[] bytes) {
		return Mono.fromCallable(() -> {
			try (PDDocument doc = Loader.loadPDF(bytes)) {
				PDFTextStripper stripper = new PDFTextStripper();
				String text = stripper.getText(doc).trim();
				if (text.isBlank()) {
					throw new IllegalStateException(
							"PDF에서 텍스트를 추출할 수 없습니다. 스캔 이미지 PDF는 지원하지 않습니다.");
				}
				return text;
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}

	private Mono<String> extractFromImage(byte[] bytes, String contentType) {
		String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);

		List<Map<String, Object>> content = List.of(
				Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
				Map.of("type", "text", "text",
						"이 이미지에 있는 텍스트를 전부 정확하게 추출해주세요. 텍스트만 출력하고 부가 설명은 하지 마세요."));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("max_tokens", 2000);
		body.put("messages", List.of(Map.of("role", "user", "content", content)));

		return callChatCompletions(body);
	}

	private Mono<String> summarize(String text) {
		String prompt = """
				다음 텍스트를 핵심 내용 중심으로 500자 이내의 한국어로 요약해주세요.
				요약문만 출력하고 부가 설명은 하지 마세요.

				텍스트:
				%s
				""".formatted(text);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("max_tokens", 600);
		body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

		return callChatCompletions(body);
	}

	private Mono<String> callChatCompletions(Map<String, Object> body) {
		return openAiWebClient.post()
				.uri("/v1/chat/completions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(root -> {
					JsonNode choices = root.path("choices");
					if (!choices.isArray() || choices.isEmpty()) {
						throw new IllegalStateException("OpenAI 응답에 choices가 없습니다.");
					}
					return choices.get(0).path("message").path("content").asText("").trim();
				})
				.onErrorMap(WebClientResponseException.class,
						ex -> new IllegalStateException("OpenAI API 오류: " + ex.getStatusCode(), ex));
	}
}
