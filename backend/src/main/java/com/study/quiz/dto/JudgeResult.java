package com.study.quiz.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JudgeResult {

	private String status;
	private String stdout;
	private String stderr;
	private String time;
	private String memory;
	private boolean correct;
}
