package com.minewaku.chatter.adapter.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.MailType;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class TemplatedEmailService implements EmailSender {
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	@Override
	public String buildContent(Map<String, String> parameters, MailType mailType) {
		try {
		    Context context = new Context();

		    parameters.forEach((key, value) -> {
		        if (key != null) {
		            context.setVariable(key.toString(), value);
		        }
		    });

		    return templateEngine.process(mailTypeToTemplateName(mailType), context);
		} catch(Exception e) {
			throw new RuntimeException("Unable to build the email content");
		}
	}

	@Override
	public void send(Email to, String subject, String content) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			
			helper.setTo(to.getValue());
			helper.setSubject(subject);
			helper.setText(content, true);
			
			mailSender.send(message);
		} catch(MessagingException e) {
			throw new RuntimeException("Unable to send an email");
		}
	}
	
	private String mailTypeToTemplateName(MailType mailType) {
		return switch (mailType) {
	        case EMAIL_CONFIRMATION -> "confirmation-email";
	        case LOGIN_NOTIFICATION -> "login-notification-email";
	        default -> throw new IllegalArgumentException("Invalid email type: " + mailType);
		};
	}
}
