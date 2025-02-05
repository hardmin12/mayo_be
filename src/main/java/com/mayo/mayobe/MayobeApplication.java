package com.mayo.mayobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mayo.mayobe")
public class MayobeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MayobeApplication.class, args);
	}

}
