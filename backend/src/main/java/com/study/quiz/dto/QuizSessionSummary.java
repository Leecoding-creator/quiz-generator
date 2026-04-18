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

	public QuizSessionSummary(QuizSession session) {
		this.id = session.getId();
		this.topic = session.getTopic();
		this.difficulty = session.getDifficulty();
		this.questionCount = session.getQuestionCount();
		this.score = session.getScore();
		this.totalCount = session.getTotalCount();
		this.createdAt = session.getCreatedAt();
	}
}
