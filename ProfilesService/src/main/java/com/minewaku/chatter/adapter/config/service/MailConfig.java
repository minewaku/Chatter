package com.minewaku.chatter.adapter.config.service;

import java.util.Properties;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

	@Bean
	JavaMailSender javaMailSender(MailProperties mailProperties) {

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		Properties props = new Properties();
		props.putAll(mailProperties.getProperties());

		mailSender.setHost(mailProperties.getHost());
		mailSender.setPort(mailProperties.getPort());
		mailSender.setUsername(mailProperties.getUsername());
		mailSender.setPassword(mailProperties.getPassword());
		
		mailSender.setJavaMailProperties(props);

		return mailSender;
	}
}
