package com.minewaku.chatter.identityaccess.start.e2e;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Mail sender that keeps messages in memory and can read the verification token out of the newest one. */
public class RecordingMailSender extends JavaMailSenderImpl {

    private static final Pattern TOKEN = Pattern.compile("token=([^\"&\\s<]+)");

    private final List<MimeMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
        sent.addAll(List.of(mimeMessages));
    }

    public int countFor(String email) {
        return (int)
                sent.stream().filter(message -> isAddressedTo(message, email)).count();
    }

    /** The token from the newest message sent to the address, if any. */
    public Optional<String> latestTokenFor(String email) {
        for (int i = sent.size() - 1; i >= 0; i--) {
            MimeMessage message = sent.get(i);
            if (isAddressedTo(message, email)) {
                Matcher matcher = TOKEN.matcher(textOf(message));
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isAddressedTo(MimeMessage message, String email) {
        try {
            Address[] to = message.getRecipients(Message.RecipientType.TO);
            if (to == null) {
                return false;
            }
            for (Address address : to) {
                if (address.toString().toLowerCase().contains(email.toLowerCase())) {
                    return true;
                }
            }
            return false;
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String textOf(Part part) {
        try {
            Object content = part.getContent();
            if (content instanceof String text) {
                return text;
            }
            if (content instanceof Multipart multipart) {
                StringBuilder all = new StringBuilder();
                for (int i = 0; i < multipart.getCount(); i++) {
                    all.append(textOf(multipart.getBodyPart(i)));
                }
                return all.toString();
            }
            return "";
        } catch (IOException | MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
