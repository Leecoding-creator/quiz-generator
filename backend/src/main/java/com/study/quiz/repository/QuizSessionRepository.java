package com.study.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.study.quiz.entity.QuizSession;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

	List<QuizSession> findAllByOrderByCreatedAtDesc();
}
