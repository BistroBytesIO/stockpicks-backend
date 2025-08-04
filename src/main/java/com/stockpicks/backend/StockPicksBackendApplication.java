package com.stockpicks.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockPicksBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockPicksBackendApplication.class, args);
	}

}
