package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.read.jdbc;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.query.UserAccountQueryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccountQueryRepository implements UserAccountQueryRepository {

    private static final String SELECT =
            "select id, normalized_email, username, birthday, status, deleted_at, created_at, updated_at"
                    + " from user_account where ";

    private final JdbcClient jdbc;

    JdbcUserAccountQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<FindUserByIdUseCase.Result> findById(UserId userId) {
        return jdbc.sql(SELECT + "id = :id")
                .param("id", userId.value())
                .query((rs, rowNum) -> new FindUserByIdUseCase.Result(
                        rs.getLong("id"),
                        rs.getString("normalized_email"),
                        rs.getString("username"),
                        rs.getObject("birthday", LocalDate.class),
                        rs.getString("status"),
                        isAccessible(rs),
                        instant(rs, "deleted_at"),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at")))
                .optional();
    }

    @Override
    public Optional<FindUserByEmailUseCase.Result> findByEmail(Email email) {
        return jdbc.sql(SELECT + "normalized_email = :email")
                .param("email", email.value())
                .query((rs, rowNum) -> new FindUserByEmailUseCase.Result(
                        rs.getLong("id"),
                        rs.getString("normalized_email"),
                        rs.getString("username"),
                        rs.getObject("birthday", LocalDate.class),
                        rs.getString("status"),
                        isAccessible(rs),
                        instant(rs, "deleted_at"),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at")))
                .optional();
    }

    private static boolean isAccessible(ResultSet rs) throws SQLException {
        return AccountStatus.valueOf(rs.getString("status")).isAccessible();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
