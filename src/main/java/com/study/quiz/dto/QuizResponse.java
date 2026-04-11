package com.study.quiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 응답을 단일 래퍼로 고정해 컨트롤러 시그니처와 클라이언트 타입이 안정된다.
 * 나중에 메타데이터(모델명, 생성 시각 등)를 넣을 여지를 남긴 형태다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {

	private List<QuizItem> items;
}
