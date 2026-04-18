package com.study.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.study.quiz.entity.CodingProblem;
import com.study.quiz.entity.CodingProblem.Category;
import com.study.quiz.entity.CodingProblem.Difficulty;

public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {

	List<CodingProblem> findByDifficulty(Difficulty difficulty);

	List<CodingProblem> findByCategory(Category category);
}
