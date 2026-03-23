package com.minewaku.chatter.identityaccess.application.port.outbound.notification;

import java.util.Map;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.MailType;

public interface EmailSender {
	void send(Email to, String subject, String content);
	String buildContent(Map<String, String> parameters, MailType mailType);
}
