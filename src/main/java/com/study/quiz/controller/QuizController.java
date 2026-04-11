package com.study.quiz.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.study.quiz.dto.QuizRequest;
import com.study.quiz.dto.QuizResponse;
import com.study.quiz.service.QuizService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * 퀴즈 생성은 외부 API 지연이 크므로 Mono로 반환해 서블릿 스레드를 오래 붙잡지 않는다.
 * 경로를 /api 아래로 묶어 정적 리소스·향후 버전과 충돌 가능성을 낮춘다.
 */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;

	@PostMapping("/generate")
	public Mono<QuizResponse> generate(@RequestBody QuizRequest request) {
		return quizService.generateQuiz(request);
	}
}
