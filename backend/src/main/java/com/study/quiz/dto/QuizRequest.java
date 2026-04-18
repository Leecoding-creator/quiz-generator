package com.study.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {

	@NotBlank(message = "주제를 입력해주세요")
	private String topic;

	private String content;

	@NotBlank(message = "난이도를 선택해주세요")
	private String difficulty;

	@Min(value = 1, message = "문항 수는 최소 1개입니다")
	@Max(value = 20, message = "문항 수는 최대 20개입니다")
	private int questionCount;
}
