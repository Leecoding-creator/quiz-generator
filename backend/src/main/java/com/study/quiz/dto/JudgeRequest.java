package com.study.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class JudgeRequest {

	@NotNull
	private Long problemId;

	@NotBlank
	private String code;

	@NotNull
	private Integer languageId;
}
