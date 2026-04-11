package com.study.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 생성 시 클라이언트가 주제·난이도·문항 수만 넘기면 되도록 묶는다.
 * 필드를 늘리면 서비스 프롬프트만 맞추면 되어 UI와 API 계약이 단순해진다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {

	/** 사용자가 원하는 학습·시험 주제(예: "JVM 가비지 컬렉션"). */
	private String topic;

	/** easy / medium / hard 등 자유 문자열 — 모델이 난이도 톤을 맞추는 데 쓴다. */
	private String difficulty;

	/** 한 번에 받을 문항 수(과도하면 토큰·지연 증가하므로 상한은 서비스에서 제한). */
	private int questionCount;
}
