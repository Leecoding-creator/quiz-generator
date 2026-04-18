package com.study.quiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 한 문항을 구조화해 두면 프론트가 객관식 UI를 그리고 채점·복습(해설·개념)까지 한 흐름으로 처리한다.
 * correctIndex는 options 범위 안의 0 기반 인덱스로 통일해 파싱 오류를 줄인다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizItem {

	private String question;

	private List<String> options;

	/** options 리스트에서 정답에 해당하는 인덱스(0부터). */
	private int correctIndex;

	/** 정답이 왜 맞는지(오답과의 차이, 복잡도·동작 근거 등) 짧게 설명해 학습 효과를 높인다. */
	private String explanation;

	/**
	 * explanation보다 긴 심층 해설. 정답 근거와(구분자 앞) 오답 보기별 오류 이유(구분자 뒤)를 담는다.
	 * 프론트는 한 줄 구분자 WRONG-ANALYSIS 구분선(프롬프트와 동일)으로 두 구간을 나눈다.
	 */
	private String detailedExplanation;

	/** 이 문항이 다루는 CS 핵심 개념 한 줄(예: "해시맵 충돌과 재해싱"). */
	private String relatedConcept;

	/**
	 * 알고리즘·코드 문항일 때 시간·공간 복잡도(Big-O) 분석을 구조적으로 담는다.
	 * 비대상 문항은 짧은 비해당 문구로 채운다.
	 */
	private String complexityAnalysis;
}
