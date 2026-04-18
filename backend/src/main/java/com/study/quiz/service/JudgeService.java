package com.study.quiz.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;

import com.study.quiz.dto.JudgeRequest;
import com.study.quiz.dto.JudgeResult;
import com.study.quiz.entity.CodingProblem;
import com.study.quiz.repository.CodingProblemRepository;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

@Service
@RequiredArgsConstructor
public class JudgeService {

	private static final String JUDGE0_URL = "https://judge0-ce.p.rapidapi.com";
	private static final String JUDGE0_HOST = "judge0-ce.p.rapidapi.com";
	private static final int TIMEOUT_SECONDS = 5;

	@Value("${judge0.api-key:}")
	private String apiKey;

	private final CodingProblemRepository problemRepository;

	public Mono<JudgeResult> submit(JudgeRequest req) {
		CodingProblem problem = problemRepository.findById(req.getProblemId())
			.orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + req.getProblemId()));

		if (apiKey == null || apiKey.isBlank()) {
			return Mono.fromCallable(() -> runLocally(req.getCode(), req.getLanguageId(), problem))
				.subscribeOn(Schedulers.boundedElastic());
		}

		return callJudge0(req, problem);
	}

	// ── 로컬 실행 ────────────────────────────────────────────────────────────

	private JudgeResult runLocally(String code, int languageId, CodingProblem problem) {
		Path tmpDir = null;
		try {
			tmpDir = Files.createTempDirectory("judge_");
			String stdin = problem.getSampleInput() == null ? "" : problem.getSampleInput();

			return switch (languageId) {
				case 62 -> runJava(code, stdin, problem.getExpectedOutput(), tmpDir);
				case 71 -> runPython(code, stdin, problem.getExpectedOutput(), tmpDir);
				case 50 -> runC(code, stdin, problem.getExpectedOutput(), tmpDir);
				default -> JudgeResult.builder().status("지원하지 않는 언어입니다").correct(false).build();
			};
		} catch (Exception e) {
			return JudgeResult.builder()
				.status("Runtime Error")
				.stderr(e.getMessage())
				.correct(false)
				.build();
		} finally {
			deleteDir(tmpDir);
		}
	}

	private JudgeResult runJava(String code, String stdin, String expected, Path dir) throws Exception {
		Path src = dir.resolve("Main.java");
		Files.writeString(src, code);

		// 컴파일
		ProcessResult compile = exec(dir, List.of("javac", src.toString()), "", false);
		if (!compile.stderr.isEmpty()) {
			return JudgeResult.builder()
				.status("Compilation Error")
				.stderr(compile.stderr)
				.correct(false)
				.build();
		}

		// 실행
		ProcessResult run = exec(dir, List.of("java", "-cp", dir.toString(), "Main"), stdin, true);
		return buildResult(run, expected);
	}

	private JudgeResult runPython(String code, String stdin, String expected, Path dir) throws Exception {
		Path src = dir.resolve("main.py");
		Files.writeString(src, code);

		String python = resolveCommand("python3", "python");
		ProcessResult run = exec(dir, List.of(python, src.toString()), stdin, true);
		return buildResult(run, expected);
	}

	private JudgeResult runC(String code, String stdin, String expected, Path dir) throws Exception {
		Path src = dir.resolve("main.c");
		Path bin = dir.resolve("main.out");
		Files.writeString(src, code);

		// 컴파일
		ProcessResult compile = exec(dir, List.of("gcc", src.toString(), "-o", bin.toString()), "", false);
		if (!compile.stderr.isEmpty()) {
			return JudgeResult.builder()
				.status("Compilation Error")
				.stderr(compile.stderr)
				.correct(false)
				.build();
		}

		// 실행
		ProcessResult run = exec(dir, List.of(bin.toString()), stdin, true);
		return buildResult(run, expected);
	}

	private JudgeResult buildResult(ProcessResult run, String expected) {
		if (run.timedOut) {
			return JudgeResult.builder().status("Time Limit Exceeded").correct(false).build();
		}
		if (!run.stderr.isEmpty()) {
			return JudgeResult.builder()
				.status("Runtime Error")
				.stdout(run.stdout)
				.stderr(run.stderr)
				.time(run.timeMs + "ms")
				.correct(false)
				.build();
		}
		boolean correct = run.stdout.trim().equals(expected == null ? "" : expected.trim());
		return JudgeResult.builder()
			.status(correct ? "Accepted" : "Wrong Answer")
			.stdout(run.stdout)
			.time(run.timeMs + "ms")
			.correct(correct)
			.build();
	}

	private ProcessResult exec(Path workDir, List<String> cmd, String stdin, boolean withStdin) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(workDir.toFile());
		pb.redirectErrorStream(false);

		long start = System.currentTimeMillis();
		Process process = pb.start();

		if (withStdin && !stdin.isEmpty()) {
			try (InputStream is = new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8))) {
				process.getOutputStream().write(is.readAllBytes());
			}
		}
		process.getOutputStream().close();

		boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		if (!finished) {
			process.destroyForcibly();
			return new ProcessResult("", "", elapsed, true);
		}

		String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		return new ProcessResult(stdout, stderr, elapsed, false);
	}

	private String resolveCommand(String... candidates) {
		for (String cmd : candidates) {
			try {
				new ProcessBuilder(cmd, "--version").start().waitFor(2, TimeUnit.SECONDS);
				return cmd;
			} catch (Exception ignored) {}
		}
		return candidates[candidates.length - 1];
	}

	private void deleteDir(Path dir) {
		if (dir == null) return;
		try {
			File[] files = dir.toFile().listFiles();
			if (files != null) for (File f : files) f.delete();
			dir.toFile().delete();
		} catch (Exception ignored) {}
	}

	private record ProcessResult(String stdout, String stderr, long timeMs, boolean timedOut) {}

	// ── Judge0 API ───────────────────────────────────────────────────────────

	private Mono<JudgeResult> callJudge0(JudgeRequest req, CodingProblem problem) {
		Map<String, Object> body = Map.of(
			"source_code", req.getCode(),
			"language_id", req.getLanguageId(),
			"stdin", problem.getSampleInput() == null ? "" : problem.getSampleInput()
		);

		@SuppressWarnings("unchecked")
		Mono<JudgeResult> result = buildClient().post()
			.uri("/submissions?base64_encoded=false&wait=true")
			.header("X-RapidAPI-Key", apiKey)
			.header("X-RapidAPI-Host", JUDGE0_HOST)
			.header("Content-Type", "application/json")
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.map(resp -> toJudge0Result(resp, problem.getExpectedOutput()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private JudgeResult toJudge0Result(Map<String, Object> resp, String expectedOutput) {
		String status = "Unknown";
		Object statusObj = resp.get("status");
		if (statusObj instanceof Map<?, ?> statusMap) {
			Object desc = statusMap.get("description");
			if (desc != null) status = desc.toString();
		}

		String stdout = nullToEmpty(resp.get("stdout"));
		String compileOutput = nullToEmpty(resp.get("compile_output"));
		String stderr = nullToEmpty(resp.get("stderr"));
		String time = nullToEmpty(resp.get("time"));
		Object memObj = resp.get("memory");
		String memory = memObj != null ? memObj.toString() : "";
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
