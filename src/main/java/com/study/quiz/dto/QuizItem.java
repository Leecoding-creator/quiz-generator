package com.study.quiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 한 문항을 구조화해 두면 프론트가 객관식 UI를 그리고 채점·해설 확장이 쉬워진다.
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
}
