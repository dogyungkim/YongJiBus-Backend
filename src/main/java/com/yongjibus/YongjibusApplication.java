package com.yongjibus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class YongjibusApplication {

	public static void main(String[] args) {
		SpringApplication.run(YongjibusApplication.class, args);
	}

}
