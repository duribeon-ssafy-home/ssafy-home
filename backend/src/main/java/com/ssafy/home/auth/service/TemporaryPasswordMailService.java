package com.ssafy.home.auth.service;

import com.ssafy.home.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TemporaryPasswordMailService {

	private static final Logger log = LoggerFactory.getLogger(TemporaryPasswordMailService.class);

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final String from;

	public TemporaryPasswordMailService(
			ObjectProvider<JavaMailSender> mailSenderProvider,
			@Value("${app.mail.from:}") String from
	) {
		this.mailSenderProvider = mailSenderProvider;
		this.from = from;
	}

	public void send(User user, String temporaryPassword) {
		log.info("Temporary password for {}: {}", user.getEmail(), temporaryPassword);

		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null || !StringUtils.hasText(from)) {
			log.info("SMTP is not configured. Temporary password was only written to the server log.");
			return;
		}

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(user.getEmail());
			message.setSubject("[SSAFY HOME] 임시 비밀번호 안내");
			message.setText("""
					비밀번호 찾기를 요청하셨습니다.

					아래 임시 비밀번호로 로그인한 뒤 마이페이지에서 새 비밀번호로 변경해 주세요.

					임시 비밀번호: %s

					본인이 요청하지 않았다면 즉시 비밀번호를 변경해 주세요.
					""".formatted(temporaryPassword));

			mailSender.send(message);
		} catch (RuntimeException exception) {
			log.warn("Failed to send temporary password email. Temporary password remains in the server log.", exception);
		}
	}
}
