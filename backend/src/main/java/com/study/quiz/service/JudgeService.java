package com.study.quiz.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.study.quiz.dto.JudgeRequest;
import com.study.quiz.dto.JudgeResult;
import com.study.quiz.entity.CodingProblem;
import com.study.quiz.repository.CodingProblemRepository;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
@RequiredArgsConstructor
public class JudgeService {

	private static final String JUDGE0_URL = "https://judge0-ce.p.rapidapi.com";
	private static final String JUDGE0_HOST = "judge0-ce.p.rapidapi.com";

	@Value("${judge0.api-key:}")
	private String apiKey;

	private final CodingProblemRepository problemRepository;

	public Mono<JudgeResult> submit(JudgeRequest req) {
		if (apiKey == null || apiKey.isBlank()) {
			return Mono.just(JudgeResult.builder()
				.status("채점 기능은 준비 중입니다")
				.correct(false)
				.build());
		}

		CodingProblem problem = problemRepository.findById(req.getProblemId())
			.orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + req.getProblemId()));

		WebClient client = buildClient();

		Map<String, Object> body = Map.of(
			"source_code", req.getCode(),
			"language_id", req.getLanguageId(),
			"stdin", problem.getSampleInput() == null ? "" : problem.getSampleInput()
		);

		@SuppressWarnings("unchecked")
		Mono<JudgeResult> result = client.post()
			.uri("/submissions?base64_encoded=false&wait=true")
			.header("X-RapidAPI-Key", apiKey)
			.header("X-RapidAPI-Host", JUDGE0_HOST)
			.header("Content-Type", "application/json")
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.map(resp -> toResult(resp, problem.getExpectedOutput()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private JudgeResult toResult(Map<String, Object> resp, String expectedOutput) {
		String status = "Unknown";
		Object statusObj = resp.get("status");
		if (statusObj instanceof Map<?, ?> statusMap) {
			Object desc = statusMap.get("description");
			if (desc != null) status = desc.toString();
		}

		String stdout = nullToEmpty(resp.get("stdout"));
		String stderr = nullToEmpty(resp.get("stderr"));
		String compileOutput = nullToEmpty(resp.get("compile_output"));
		String time = nullToEmpty(resp.get("time"));
		Object memObj = resp.get("memory");
		String memory = memObj != null ? memObj.toString() : "";

		if (!stderr.isEmpty() && compileOutput.isEmpty()) {
			// prefer compile_output for display when both present
		}
		String displayErr = compileOutput.isEmpty() ? stderr : compileOutput;

		boolean correct = !stdout.isEmpty()
			&& stdout.trim().equals(expectedOutput == null ? "" : expectedOutput.trim());

		return JudgeResult.builder()
			.status(status)
			.stdout(stdout)
			.stderr(displayErr)
			.time(time)
			.memory(memory)
			.correct(correct)
			.build();
	}

	private String nullToEmpty(Object val) {
		return val == null ? "" : val.toString();
	}

	private WebClient buildClient() {
		HttpClient httpClient = HttpClient.create()
			.responseTimeout(Duration.ofSeconds(30))
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

		return WebClient.builder()
			.baseUrl(JUDGE0_URL)
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
	}
}
