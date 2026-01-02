package com.minewaku.chatter.adapter.messaging.publisher;

import com.minewaku.chatter.domain.event.core.DomainEvent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrichedDomainEvent<T extends DomainEvent> {
    private String aggregateType;
    private Long aggregateId;
    
    private T domainEvent;

    public String getDomainEventType() {
        String simpleName = domainEvent.getClass().getSimpleName();
        return simpleName
                .replaceAll("([a-z])([A-Z]+)", "$1_$2") // chèn dấu '_' giữa lower và upper
                .toUpperCase();
    }
}