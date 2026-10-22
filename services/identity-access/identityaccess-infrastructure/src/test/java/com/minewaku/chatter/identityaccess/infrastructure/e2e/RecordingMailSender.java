package com.minewaku.chatter.identityaccess.infrastructure.e2e;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Mail sender that keeps messages in memory instead of contacting an SMTP server. */
public class RecordingMailSender extends JavaMailSenderImpl {

    private final List<MimeMessage> sent = new CopyOnWriteArrayList<>();

    public List<MimeMessage> sent() {
        return sent;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
        sent.addAll(List.of(mimeMessages));
    }
}
