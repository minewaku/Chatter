package com.minewaku.chatter.identityaccess.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.authentication.api.command.AuthenticateUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * SC-003 and SC-004: register, verify and sign in through the real beans, restart the application against the same
 * stores, and check that no plain secret is stored or logged.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = E2eTestApplication.class)
class EndToEndPersistenceTest {

    private static final String PASSWORD = "S3cure-pass!";
    private static final Pattern TOKEN = Pattern.compile("token=([^\"<&\\s]+)");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        E2eProperties.values().forEach((key, value) -> registry.add(key, () -> value));
    }

    @Autowired
    RegisterUserUseCase register;

    @Autowired
    VerifyEmailUseCase verify;

    @Autowired
    AuthenticateUseCase authenticate;

    @Autowired
    RecordingMailSender mail;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    JdbcClient jdbc;

    @Test
    void theFlowSurvivesARestartAndNoPlainSecretIsStoredOrLogged(CapturedOutput output) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String email = "e2e" + suffix + "@example.com";

        RegisterUserUseCase.Result registered = register.handle(
                new RegisterUserUseCase.Command(email, "e2e" + suffix, LocalDate.of(1999, 1, 1), PASSWORD));
        assertThat(registered).isNotInstanceOf(RegisterUserUseCase.AccountAlreadyExists.class);

        String verificationToken = verificationTokenSentTo(email);
        assertThat(verify.handle(new VerifyEmailUseCase.Command(verificationToken)))
                .isInstanceOf(VerifyEmailUseCase.Verified.class);

        AuthenticateUseCase.Authenticated tokens = authenticate.handle(
                new AuthenticateUseCase.Command(email, PASSWORD, new AuthenticateUseCase.DeviceInfo("e2e-device")));
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        long userId = jdbc.sql("select id from user_account where normalized_email = :email")
                .param("email", email)
                .query(Long.class)
                .single();

        // SC-003: restart the application against the same stores and look everything up again.
        try (ConfigurableApplicationContext restarted = new SpringApplicationBuilder(E2eTestApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(E2eProperties.values())
                .properties("server.port=0")
                .run()) {
            Optional<FindUserByEmailUseCase.Result> account =
                    restarted.getBean(FindUserByEmailUseCase.class).handle(new FindUserByEmailUseCase.Query(email));
            assertThat(account).isPresent();
            assertThat(account.get().userId()).isEqualTo(userId);
            assertThat(account.get().status()).isEqualTo("ACTIVE");

            assertThat(restarted
                            .getBean(VerifyEmailUseCase.class)
                            .handle(new VerifyEmailUseCase.Command(verificationToken)))
                    .isInstanceOf(VerifyEmailUseCase.AlreadyVerified.class);

            List<FindSessionsQuery.SessionView> sessions =
                    restarted.getBean(FindSessionsQuery.class).handle(new FindSessionsQuery.Command(userId));
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).deviceInfo()).isEqualTo("e2e-device");
            assertThat(sessions.get(0).revokedAt()).isNull();
        }

        // SC-004: no plain password, raw verification token or raw refresh credential in storage or logs.
        List<String> secrets = List.of(PASSWORD, verificationToken, tokens.refreshToken());
        String accountRow = jdbc.sql("select t::text from user_account t where id = :id")
                .param("id", userId)
                .query(String.class)
                .single();
        assertThat(accountRow).doesNotContain(secrets);
        for (String redisText : redisDump()) {
            assertThat(redisText).doesNotContain(secrets);
        }
        assertThat(output.getAll()).doesNotContain(secrets).doesNotContain(tokens.accessToken());
    }

    private String verificationTokenSentTo(String email) throws Exception {
        for (MimeMessage message : mail.sent()) {
            if (message.getAllRecipients()[0].toString().equals(email)) {
                Matcher matcher = TOKEN.matcher(String.valueOf(message.getContent()));
                assertThat(matcher.find())
                        .as("verification link in the email body")
                        .isTrue();
                return matcher.group(1);
            }
        }
        throw new AssertionError("no verification email was sent to " + email);
    }

    /** Every key and value under this test's key prefix, rendered as text. */
    private List<String> redisDump() {
        List<String> dump = new ArrayList<>();
        Set<String> keys = redis.keys(E2eProperties.KEY_PREFIX + "*");
        for (String key : keys) {
            dump.add(key);
            String type = String.valueOf(redis.type(key).code());
            switch (type) {
                case "hash" -> dump.add(String.valueOf(redis.opsForHash().entries(key)));
                case "zset" -> dump.add(String.valueOf(redis.opsForZSet().range(key, 0, -1)));
                default -> dump.add(String.valueOf(redis.opsForValue().get(key)));
            }
        }
        assertThat(keys).as("keys written under the e2e prefix").isNotEmpty();
        return dump;
    }
}
