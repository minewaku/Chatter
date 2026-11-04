package com.minewaku.chatter.profilenext.start;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.start.support.StartTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class StartSmokeTest extends StartTestSupport {

    @Test
    void theServiceStartsAndReportsItselfHealthy() throws Exception {
        HttpResponse<String> health = send("GET", "/actuator/health", null, null);

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void aProfileRequestWithoutATokenIsRefusedWithTheStandardBody() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/profiles/@me", null, null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"unauthenticated\"");
    }
}
