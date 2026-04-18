package com.study.quiz.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.study.quiz.dto.JudgeRequest;
import com.study.quiz.dto.JudgeResult;
import com.study.quiz.service.JudgeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/judge")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
@RequiredArgsConstructor
public class JudgeController {

	private final JudgeService judgeService;

	@PostMapping("/submit")
	public Mono<JudgeResult> submit(@Valid @RequestBody JudgeRequest request) {
		return judgeService.submit(request);
	}
}
