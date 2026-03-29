package com.minewaku.chatter.identityaccess.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.minewaku.chatter"})
public class StartApplication {
	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}
}
