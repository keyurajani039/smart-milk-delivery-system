package com.example.milkdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MilkdeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(MilkdeliveryApplication.class, args);
	}

}
