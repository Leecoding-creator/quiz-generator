package com.study.quiz.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.study.quiz.entity.CodingProblem;
import com.study.quiz.entity.CodingProblem.Category;
import com.study.quiz.entity.CodingProblem.Difficulty;
import com.study.quiz.repository.CodingProblemRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
@RequiredArgsConstructor
public class CodingProblemController {

	private final CodingProblemRepository repository;

	@GetMapping
	public List<CodingProblem> getAll(
		@RequestParam(required = false) Difficulty difficulty,
		@RequestParam(required = false) Category category
	) {
		if (difficulty != null) return repository.findByDifficulty(difficulty);
		if (category != null) return repository.findByCategory(category);
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<CodingProblem> getById(@PathVariable Long id) {
		return repository.findById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/random")
	public ResponseEntity<CodingProblem> getRandom() {
		List<CodingProblem> all = repository.findAll();
		if (all.isEmpty()) return ResponseEntity.notFound().build();
		int idx = (int) (Math.random() * all.size());
		return ResponseEntity.ok(all.get(idx));
	}
}
