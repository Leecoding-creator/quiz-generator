package com.study.quiz.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScoreUpdateRequest {

	private int score;
	private List<Integer> wrongIndexes;
}
