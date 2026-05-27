package com.project.optrabidz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OptrabidzApplication {

	public static void main(String[] args) {
		SpringApplication.run(OptrabidzApplication.class, args);
	}

}
