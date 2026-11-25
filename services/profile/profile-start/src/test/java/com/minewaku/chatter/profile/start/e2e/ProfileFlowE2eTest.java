package com.minewaku.chatter.profile.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profile.start.support.StartTestSupport;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ProfileFlowE2eTest extends StartTestSupport {

    private static final AtomicLong IDS = new AtomicLong(5_000_000L);

    private static long freshUser() {
        return IDS.incrementAndGet();
    }

    private HttpResponse<String> read(long asUser, String who) throws Exception {
        return send("GET", "/api/v1/profiles/" + who, bearer(asUser), null);
    }

    private HttpResponse<String> patch(long asUser, String json) throws Exception {
        return send("PATCH", "/api/v1/profiles/@me", bearer(asUser), json);
    }

    @Test
    void readThenEditNameThenBioShowsBothAndStartsFromDefaults() throws Exception {
        long user = freshUser();

        HttpResponse<String> defaults = read(user, "@me");
        assertThat(defaults.statusCode()).isEqualTo(200);
        assertThat(defaults.body())
                .contains("\"userId\":" + user)
                .contains("\"displayName\":\"\"")
                .contains("\"bio\":\"\"")
                .contains("\"avatarUrl\":null")
                .contains("\"createdAt\":null");

        assertThat(patch(user, "{\"displayName\":\"Mine\"}").statusCode()).isEqualTo(204);
        assertThat(read(user, "@me").body()).contains("\"displayName\":\"Mine\"").contains("\"bio\":\"\"");

        assertThat(patch(user, "{\"bio\":\"Hello\"}").statusCode()).isEqualTo(204);
        HttpResponse<String> both = read(user, Long.toString(user));
        assertThat(both.body()).contains("\"displayName\":\"Mine\"").contains("\"bio\":\"Hello\"");
        assertThat(both.body()).doesNotContain("\"createdAt\":null");
    }

    @Test
    void aRefusedSaveChangesNothing() throws Exception {
        long user = freshUser();
        patch(user, "{\"displayName\":\"Mine\"}");

        HttpResponse<String> refused = patch(user, "{\"displayName\":\"" + "x".repeat(33) + "\"}");

        assertThat(refused.statusCode()).isEqualTo(400);
        assertThat(refused.body())
                .contains("\"code\":\"validation_failed\"")
                .contains("Display name must be 1 to 32 characters.")
                .doesNotContain("xxxxx");
        assertThat(read(user, "@me").body()).contains("\"displayName\":\"Mine\"");
    }

    @Test
    void oneUsersTokenCanNeverChangeAnotherUsersRow() throws Exception {
        long alice = freshUser();
        long bob = freshUser();
        patch(bob, "{\"displayName\":\"Bob\"}");

        // a body that names someone else is ignored: the write goes to the token's own row
        assertThat(patch(alice, "{\"displayName\":\"Alice\",\"userId\":" + bob + "}").statusCode())
                .isEqualTo(204);
        // and there is no endpoint that writes to a path id
        assertThat(send("PATCH", "/api/v1/profiles/" + bob, bearer(alice), "{\"displayName\":\"Hacked\"}")
                        .statusCode())
                .isIn(404, 405);

        assertThat(read(alice, Long.toString(bob)).body()).contains("\"displayName\":\"Bob\"");
        assertThat(read(bob, "@me").body()).contains("\"displayName\":\"Bob\"");
        assertThat(read(bob, Long.toString(alice)).body()).contains("\"displayName\":\"Alice\"");
    }

    @Test
    void readsForAnyExistingIdAlwaysSucceed() throws Exception {
        long caller = freshUser();

        for (long id : new long[] {1L, caller, 123456789012L}) {
            assertThat(read(caller, Long.toString(id)).statusCode()).isEqualTo(200);
        }
        assertThat(read(caller, "abc").statusCode()).isEqualTo(400);
    }

    @Test
    void anExpiredTokenIsRefused() throws Exception {
        String expired = "Bearer " + KEYS.token(freshUser(), Instant.now().minusSeconds(3600));

        HttpResponse<String> response = send("GET", "/api/v1/profiles/@me", expired, null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"unauthenticated\"");
    }

    @Test
    void aTokenForAnAccountClosedElsewhereIsStillAcceptedUntilItExpires() throws Exception {
        // No revocation list is kept (FR-006): the token's signature and expiry are all that is checked.
        long closedAccount = freshUser();

        assertThat(read(closedAccount, "@me").statusCode()).isEqualTo(200);
        assertThat(patch(closedAccount, "{\"bio\":\"still works\"}").statusCode()).isEqualTo(204);
    }

    @Test
    void aTokenFromAnotherIssuerIsRefused() throws Exception {
        String foreign = "Bearer " + KEYS.token("42", "someone-else", Instant.now().plusSeconds(600));

        assertThat(send("GET", "/api/v1/profiles/@me", foreign, null).statusCode()).isEqualTo(401);
    }
}
