package com.study.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadResponse {

	/** 파일에서 추출한 원문 텍스트. */
	private String extractedText;

	/** 추출 텍스트를 GPT로 요약한 결과 (500자 이내). 퀴즈 생성 시 content 필드로 사용. */
	private String summary;
}
