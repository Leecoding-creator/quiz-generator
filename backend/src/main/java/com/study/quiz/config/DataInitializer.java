package com.study.quiz.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.study.quiz.entity.CodingProblem;
import com.study.quiz.entity.CodingProblem.Category;
import com.study.quiz.entity.CodingProblem.Difficulty;
import com.study.quiz.entity.CodingProblem.Language;
import com.study.quiz.repository.CodingProblemRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final CodingProblemRepository repository;

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) return;

		LocalDateTime now = LocalDateTime.now();

		repository.saveAll(List.of(
			CodingProblem.builder()
				.title("두 수의 합")
				.description("두 정수 A와 B가 주어졌을 때, A+B를 출력하는 프로그램을 작성하시오.")
				.inputDescription("첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)")
				.outputDescription("첫째 줄에 A+B를 출력한다.")
				.sampleInput("1 2")
				.sampleOutput("3")
				.expectedOutput("3")
				.difficulty(Difficulty.EASY)
				.category(Category.MATH)
				.languageSupport(Language.JAVA)
				.timeLimit(1000)
				.memoryLimit(256)
				.createdAt(now)
				.build(),

			CodingProblem.builder()
				.title("최댓값 찾기")
				.description("N개의 정수가 주어질 때, 그 중 최댓값을 출력하는 프로그램을 작성하시오.")
				.inputDescription("첫째 줄에 공백으로 구분된 N개의 정수가 주어진다.")
				.outputDescription("최댓값을 출력한다.")
				.sampleInput("3 1 4 1 5 9 2 6")
				.sampleOutput("9")
				.expectedOutput("9")
				.difficulty(Difficulty.EASY)
				.category(Category.ARRAY)
				.languageSupport(Language.JAVA)
				.timeLimit(1000)
				.memoryLimit(256)
				.createdAt(now)
				.build(),

			CodingProblem.builder()
				.title("문자열 뒤집기")
				.description("문자열 S가 주어졌을 때, 이를 뒤집어서 출력하는 프로그램을 작성하시오.")
				.inputDescription("첫째 줄에 문자열 S가 주어진다. (1 ≤ |S| ≤ 100)")
				.outputDescription("첫째 줄에 뒤집은 문자열을 출력한다.")
				.sampleInput("hello")
				.sampleOutput("olleh")
				.expectedOutput("olleh")
				.difficulty(Difficulty.EASY)
				.category(Category.STRING)
				.languageSupport(Language.JAVA)
				.timeLimit(1000)
				.memoryLimit(256)
				.createdAt(now)
				.build(),

			CodingProblem.builder()
				.title("피보나치 수열")
				.description("피보나치 수열의 N번째 항을 출력하는 프로그램을 작성하시오. F(1)=1, F(2)=1, F(N)=F(N-1)+F(N-2).")
				.inputDescription("첫째 줄에 N이 주어진다. (1 ≤ N ≤ 45)")
				.outputDescription("N번째 피보나치 수를 출력한다.")
				.sampleInput("10")
				.sampleOutput("55")
				.expectedOutput("55")
				.difficulty(Difficulty.MEDIUM)
				.category(Category.DP)
				.languageSupport(Language.JAVA)
				.timeLimit(1000)
				.memoryLimit(256)
				.createdAt(now)
				.build(),

			CodingProblem.builder()
				.title("버블 정렬")
				.description("N개의 정수를 버블 정렬 알고리즘을 이용하여 오름차순으로 정렬하는 프로그램을 작성하시오.")
				.inputDescription("첫째 줄에 공백으로 구분된 N개의 정수가 주어진다. (1 ≤ N ≤ 1000)")
				.outputDescription("정렬된 수를 공백으로 구분하여 출력한다.")
				.sampleInput("5 3 1 4 2")
				.sampleOutput("1 2 3 4 5")
				.expectedOutput("1 2 3 4 5")
				.difficulty(Difficulty.MEDIUM)
				.category(Category.SORT)
				.languageSupport(Language.JAVA)
				.timeLimit(2000)
				.memoryLimit(256)
				.createdAt(now)
				.build()
		));
	}
}
