package com.github.dogyungkim.yongjibus.yongjibus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@SpringBootApplication
public class YongjibusApplication {

	public static void main(String[] args) {
		SpringApplication.run(YongjibusApplication.class, args);
	}

}
