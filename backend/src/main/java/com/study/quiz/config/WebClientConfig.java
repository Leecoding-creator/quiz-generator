package com.study.quiz.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * Gemini 호출은 I/O 바운드라 WebClient로 논블로킹 처리하고, 타임아웃으로 무한 대기를 막는다.
 * baseUrl을 빈으로 고정해 엔드포인트 경로만 서비스에서 조합하면 테스트 시 교체가 쉽다.
 */
@Configuration
public class WebClientConfig {

	@Bean
	public WebClient geminiWebClient(
			@Value("${gemini.base-url}") String baseUrl,
			@Value("${gemini.http.connect-timeout-ms:10000}") int connectTimeoutMs,
			@Value("${gemini.http.response-timeout-ms:120000}") int responseTimeoutMs) {

		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofMillis(responseTimeoutMs))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs);

		return WebClient.builder()
				.baseUrl(baseUrl)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
