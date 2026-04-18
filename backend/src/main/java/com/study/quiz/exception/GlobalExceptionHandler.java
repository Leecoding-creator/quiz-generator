package com.study.quiz.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(WebClientResponseException.class)
	public ResponseEntity<Map<String, String>> handleWebClientResponse(WebClientResponseException ex) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of("message", "AI API 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("message", "입력값이 올바르지 않습니다: " + detail));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("message", "서비스 처리 중 오류가 발생했습니다: " + ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleException(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("message", "예기치 않은 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
	}
}
