package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@Table("account")
public class JdbcUserEntity extends BaseEntity implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    @Column("email")
    private String email;

    @Column("username")
    private String username;

    @Column("birthday")
    private LocalDate birthday;

    @Column("is_enabled")
    private Boolean enabled;

    @Column("is_locked")
    private Boolean locked;

    @Column("is_deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private Instant deletedAt;

    @Column("hashed_password")
    private String hashedPassword;

    @Column("password_modified_at")
    private Instant passwordModifiedAt;

    @Column("algorithm")
    private String algorithm;

    @Column("salt")
    private byte[] salt;

    public JdbcUserEntity() {
        super();
    }



    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return hashedPassword;
    }
}