package com.helps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelpsApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpsApplication.class, args);
	}

}
