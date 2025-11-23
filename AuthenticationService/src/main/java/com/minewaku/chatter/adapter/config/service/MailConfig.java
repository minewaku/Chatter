package com.minewaku.chatter.adapter.config.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.minewaku.chatter.adapter.config.properties.MailProperties;

@Configuration
public class MailConfig {
    	
	@Bean
	JavaMailSender javaMailSender(MailProperties mailProperties) {

	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

	    mailSender.setUsername(mailProperties.getUsername());
	    mailSender.setPassword(mailProperties.getPassword());
	    return mailSender;
	}
}
