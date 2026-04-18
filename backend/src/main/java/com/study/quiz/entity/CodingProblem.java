package com.study.quiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coding_problem")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingProblem {

	public enum Difficulty { EASY, MEDIUM, HARD }
	public enum Category { STRING, ARRAY, SORT, DP, GRAPH, MATH }
	public enum Language { JAVA, PYTHON, C }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String inputDescription;

	@Column(columnDefinition = "TEXT")
	private String outputDescription;

	@Column(columnDefinition = "TEXT")
	private String sampleInput;

	@Column(columnDefinition = "TEXT")
	private String sampleOutput;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String expectedOutput;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Difficulty difficulty;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Language languageSupport;

	private int timeLimit;

	private int memoryLimit;

	@Column(nullable = false)
	private LocalDateTime createdAt;
}
