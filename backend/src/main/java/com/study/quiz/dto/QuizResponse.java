package com.study.quiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {

	/** 퀴즈 생성 시 DB에 저장된 세션 ID. 히스토리 조회/재시도 응답에서는 null. */
	private Long sessionId;

	private List<QuizItem> items;

	public QuizResponse(List<QuizItem> items) {
		this.sessionId = null;
		this.items = items;
	}
}
