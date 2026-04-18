package com.study.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.study.quiz.entity.QuizItemEntity;

public interface QuizItemRepository extends JpaRepository<QuizItemEntity, Long> {

	List<QuizItemEntity> findBySessionId(Long sessionId);
}
