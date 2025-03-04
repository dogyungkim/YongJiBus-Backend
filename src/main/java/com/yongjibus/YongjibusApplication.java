package com.yongjibus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.auth.domain.Member;
import lombok.RequiredArgsConstructor;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
public class YongjibusApplication implements CommandLineRunner{

	private final AuthService authService;

	private static final String email = "test@test.com";
	private static final String username = "김도경";
	private static final String name = "김도경";
	private static final String password = "12341234";

	private static final String email2 = "test2@mju.ac.kr";
	private static final String username2 = "이영지";
	private static final String name2 = "이영지";
	private static final String password2 = "12341234";

	public static void main(String[] args) {
		SpringApplication.run(YongjibusApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		String authCode = authService.sendAuthEmail(email);
		authService.verifyAuthCode(email, authCode);

		String authCode2 = authService.sendAuthEmail(email2);
		authService.verifyAuthCode(email2, authCode2);

		Member member = Member.builder()
			.email(email)
			.username(username)
			.name(name)
			.password(password)
			.build();

		Member member2 = Member.builder()
			.email(email2)
			.username(username2)
			.name(name2)
			.password(password2)
			.build();

		System.out.println(authService.signup(member));
		System.out.println(authService.signup(member2));
	}
}
