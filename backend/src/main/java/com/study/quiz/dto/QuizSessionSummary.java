package com.study.quiz.dto;

import java.time.LocalDateTime;

import com.study.quiz.entity.QuizSession;

import lombok.Getter;

@Getter
public class QuizSessionSummary {

	private final Long id;
	private final String topic;
	private final String difficulty;
	private final int questionCount;
	private final int score;
	private final int totalCount;
	private final LocalDateTime createdAt;
	/** 틀린 문항 수. 0이면 채점 전이거나 전부 맞음. */
	private final int wrongCount;

	public QuizSessionSummary(QuizSession session) {
		this.id = session.getId();
		this.topic = session.getTopic();
		this.difficulty = session.getDifficulty();
		this.questionCount = session.getQuestionCount();
		this.score = session.getScore();
		this.totalCount = session.getTotalCount();
		this.createdAt = session.getCreatedAt();
		this.wrongCount = countWrong(session.getWrongIndexes());
	}

	private static int countWrong(String json) {
		if (json == null || json.isBlank() || "[]".equals(json.trim())) return 0;
		return (int) json.chars().filter(c -> c == ',').count() + 1;
	}
}
