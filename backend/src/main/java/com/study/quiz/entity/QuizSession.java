package com.study.quiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_session")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String topic;

	@Column(nullable = false)
	private String difficulty;

	private int questionCount;

	private int score;

	private int totalCount;

	@Column(nullable = false)
	private LocalDateTime createdAt;
}
