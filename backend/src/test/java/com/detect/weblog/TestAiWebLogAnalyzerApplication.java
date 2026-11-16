package com.detect.weblog;

import org.springframework.boot.SpringApplication;

public class TestAiWebLogAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.from(com.detect.weblog.AiWebLogAnalyzerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
