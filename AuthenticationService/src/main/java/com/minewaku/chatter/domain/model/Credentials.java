package com.minewaku.chatter.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class Credentials {
	
    @NonNull
    private final UserId userId;
    
    @NonNull @Setter
    private HashedPassword hashedPassword;
    
    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();
    

    private Credentials(@NonNull UserId userId, @NonNull HashedPassword hashedPassword) {
        this.userId = userId;
        this.hashedPassword = hashedPassword;
    }
    

    static public Credentials createNew(@NonNull UserId userId, @NonNull HashedPassword hashedPassword) {
    	Credentials credentials = new Credentials(userId, hashedPassword);
    	CreateConfirmationTokenDomainEvent createConfirmationTokenDomainEvent = new CreateConfirmationTokenDomainEvent(userId, null);
    	credentials.events.add(createConfirmationTokenDomainEvent);
    	return credentials;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Credentials)) return false;
        Credentials that = (Credentials) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "userId=" + userId +
                ", hashedPassword=[PROTECTED]" +
                '}';
    }
}
