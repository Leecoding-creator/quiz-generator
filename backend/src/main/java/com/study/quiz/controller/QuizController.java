package com.study.quiz.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.study.quiz.dto.QuizRequest;
import com.study.quiz.dto.QuizResponse;
import com.study.quiz.dto.QuizSessionSummary;
import com.study.quiz.dto.UploadResponse;
import com.study.quiz.service.FileUploadService;
import com.study.quiz.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * 퀴즈 생성은 외부 API 지연이 크므로 Mono로 반환해 서블릿 스레드를 오래 붙잡지 않는다.
 * 경로를 /api 아래로 묶어 정적 리소스·향후 버전과 충돌 가능성을 낮춘다.
 */
@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;
	private final FileUploadService fileUploadService;

	@PostMapping("/generate")
	public Mono<QuizResponse> generate(@Valid @RequestBody QuizRequest request) {
		return quizService.generateQuiz(request);
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Mono<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
		return fileUploadService.processUpload(file);
	}

	@GetMapping("/history")
	public List<QuizSessionSummary> getHistory() {
		return quizService.getHistory();
	}

	@GetMapping("/history/{sessionId}")
	public QuizResponse getHistoryDetail(@PathVariable Long sessionId) {
		return quizService.getHistoryDetail(sessionId);
	}
}
