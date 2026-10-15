package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.read.jdbc.JdbcUserAccountQueryRepository;
import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import com.minewaku.chatter.identityaccess.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** SC-010: mapping an account row to the model adds under 1% to a real database lookup. */
@Tag("measurement")
@SpringBootTest(classes = JdbcTestConfiguration.class)
class MappingCostMeasurementTest extends PostgreSqlTestSupport {

    private static final int LOOKUPS = 2_000;
    private static final int MAPPINGS = 200_000;

    @Autowired
    JdbcUserAccountRepository writer;

    @Autowired
    JdbcUserAccountQueryRepository reader;

    @Autowired
    JdbcClient jdbc;

    @Test
    void mappingCostIsUnderOnePercentOfALookup() {
        long id = System.currentTimeMillis() * 1000 + 900_000;
        writer.save(AccountFixtures.pendingAccount(id, "measure" + id + "@example.com", "measure" + id));
        Email email = new Email("measure" + id + "@example.com");
        UserAccountRowMapper mapper = new UserAccountRowMapper();
        UserAccountRow row = mapper.toRow(AccountFixtures.reconstitutedAccount(1L, AccountStatus.ACTIVE, null));

        for (int i = 0; i < 500; i++) {
            reader.findByEmail(email);
        }
        for (int i = 0; i < 20_000; i++) {
            mapper.toModel(row);
        }

        long lookupStart = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            reader.findByEmail(email);
        }
        double lookupNanos = (System.nanoTime() - lookupStart) / (double) LOOKUPS;

        long mapStart = System.nanoTime();
        for (int i = 0; i < MAPPINGS; i++) {
            mapper.toModel(row);
        }
        double mapNanos = (System.nanoTime() - mapStart) / (double) MAPPINGS;

        System.out.printf(
                "SC-010 measurement: lookup=%.1f us, mapping=%.3f us, ratio=%.4f%%%n",
                lookupNanos / 1000, mapNanos / 1000, 100 * mapNanos / lookupNanos);
        assertThat(mapNanos).isLessThan(lookupNanos * 0.01);
    }
}
