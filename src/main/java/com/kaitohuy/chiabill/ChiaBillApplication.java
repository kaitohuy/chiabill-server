package com.kaitohuy.chiabill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChiaBillApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChiaBillApplication.class, args);
		System.out.println("DB_URL=" + System.getenv("DB_URL"));
	}

}
