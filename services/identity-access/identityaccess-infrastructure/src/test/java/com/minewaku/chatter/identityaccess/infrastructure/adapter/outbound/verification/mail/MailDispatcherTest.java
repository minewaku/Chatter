package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(OutputCaptureExtension.class)
class MailDispatcherTest {

    private static final String SECRET = "super-secret-raw-token";

    private JavaMailSender sender;
    private CircuitBreaker breaker;
    private MailDispatcher dispatcher;
    private MimeMessage message;

    @BeforeEach
    void setUp() throws Exception {
        sender = mock(JavaMailSender.class);
        breaker = CircuitBreaker.of(
                "mail-dispatcher-test",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(2)
                        .minimumNumberOfCalls(2)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMillis(300))
                        .permittedNumberOfCallsInHalfOpenState(1)
                        .build());
        dispatcher = new MailDispatcher(
                sender,
                breaker,
                RetryPolicy.builder()
                        .maxRetries(3)
                        .delay(Duration.ofMillis(1))
                        .includes(MailException.class)
                        .build());
        message = new MimeMessage((Session) null);
        message.setText("link with " + SECRET);
    }

    @Test
    void aSuccessfulSendCallsTheSenderOnce() {
        doNothing().when(sender).send(any(MimeMessage.class));

        dispatcher.send(message);

        verify(sender, times(1)).send(message);
        assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
    }

    @Test
    void aTransientFailureIsRetriedAndTheBreakerSeesOneSuccess() {
        doThrow(new MailSendException("smtp down"))
                .doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(sender)
                .send(any(MimeMessage.class));

        dispatcher.send(message);

        verify(sender, times(3)).send(message);
        assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void aSendThatFailsAfterItsRetriesCountsOnceTowardTheBreakerAndSurfacesAsMailDeliveryException() {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> dispatcher.send(message)).isExactlyInstanceOf(MailDeliveryException.class);

        verify(sender, times(4)).send(message);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void aFailureThatIsNotAMailExceptionIsNotRetried() {
        doThrow(new IllegalStateException("broken")).when(sender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> dispatcher.send(message)).isExactlyInstanceOf(MailDeliveryException.class);

        verify(sender, times(1)).send(message);
    }

    @Test
    void afterRepeatedFailuresTheBreakerOpensAndNothingIsSentWhileItIsOpen() {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> dispatcher.send(message)).isInstanceOf(MailDeliveryException.class);
        assertThatThrownBy(() -> dispatcher.send(message)).isInstanceOf(MailDeliveryException.class);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        org.mockito.Mockito.clearInvocations(sender);

        assertThatThrownBy(() -> dispatcher.send(message)).isExactlyInstanceOf(MailDeliveryException.class);

        verifyNoInteractions(sender);
    }

    @Test
    void afterTheOpenDurationAGoodSendClosesTheBreakerAgain() throws InterruptedException {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> dispatcher.send(message)).isInstanceOf(MailDeliveryException.class);
        assertThatThrownBy(() -> dispatcher.send(message)).isInstanceOf(MailDeliveryException.class);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        org.mockito.Mockito.reset(sender);
        doNothing().when(sender).send(any(MimeMessage.class));

        Thread.sleep(450);
        dispatcher.send(message);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void failuresNeverExposeTheMessageContentInAnyExceptionOrLog(CapturedOutput output) {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> dispatcher.send(message));

        assertThat(failure).isInstanceOf(MailDeliveryException.class);
        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertThat(String.valueOf(t.getMessage())).doesNotContain(SECRET);
        }
        assertThat(output.getAll()).doesNotContain(SECRET);
    }
}
