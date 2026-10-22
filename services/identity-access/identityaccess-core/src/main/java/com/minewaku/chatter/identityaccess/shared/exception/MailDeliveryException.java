package com.minewaku.chatter.identityaccess.shared.exception;

/**
 * A notifier could not deliver mail: the send failed after its retries, or sending is being held off. The message is
 * fixed and never carries an address, token, or link.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(Throwable cause) {
        super("Mail could not be delivered", cause);
    }

    public MailDeliveryException() {
        super("Mail could not be delivered");
    }
}
