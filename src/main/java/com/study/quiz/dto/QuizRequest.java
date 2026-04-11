package com.study.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 생성 시 주제·난이도·문항 수와 함께 학습 원문을 넘기면 맞춤 문항으로 이어진다.
 * 필드를 늘리면 서비스 프롬프트만 맞추면 되어 UI와 API 계약이 단순해진다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {

	/** 사용자가 원하는 학습·시험 주제(예: "JVM 가비지 컬렉션"). */
	private String topic;

	/**
	 * 사용자가 공부한 노트·코드·요약 등 원문; 비우면 주제만으로 범위를 잡는다.
	 * 실기형 문항을 붙이려면 여기에 스니펫을 넣는 편이 정확도가 오른다.
	 */
	private String content;

	/** easy / medium / hard 등 자유 문자열 — 모델이 난이도 톤을 맞추는 데 쓴다. */
	private String difficulty;

	/** 한 번에 받을 문항 수(과도하면 토큰·지연 증가하므로 상한은 서비스에서 제한). */
	private int questionCount;
}
