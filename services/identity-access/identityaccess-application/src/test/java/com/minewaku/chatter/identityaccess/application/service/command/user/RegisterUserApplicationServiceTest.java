package com.minewaku.chatter.identityaccess.application.service.command.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Birthday;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Username;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.RegisterDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

@ExtendWith(MockitoExtension.class)
class RegisterUserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterDomainService registerDomainService;

    @Mock
    private UniqueStringIdGenerator uniqueStringIdGenerator;

    @Mock
    private EventQueue eventQueue;

    @Mock
    private OutboxStore outboxStore;

    private RegisterUserApplicationService service;

    @Test
    void shouldPublishDomainAndIntegrationEventsWhenUserIsRegistered() {
        service = new RegisterUserApplicationService(
                userRepository,
                registerDomainService,
                uniqueStringIdGenerator,
                new DomainEventPublisher(eventQueue),
                new IntegrationEventPublisher(outboxStore)
        );

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "user@example.com",
                "user",
                LocalDate.of(1990, Month.JANUARY, 1),
                "Password123!"
        );

        User user = User.register(
                new UserId(1L),
                new Email("user@example.com"),
                new Username("user"),
                new Birthday(LocalDate.of(1990, Month.JANUARY, 1)),
                new HashedPassword("bcrypt", "hashed-password", new byte[]{1, 2, 3})
        );

        when(registerDomainService.handle(any(), any(), any(), any())).thenReturn(user);
        when(uniqueStringIdGenerator.generate()).thenReturn("event-id");

        service.handle(command);

        verify(userRepository).save(user);
        verify(eventQueue).push(anyList());
        verify(outboxStore).push(any(IntegrationEventWrapper.class));
    }
}
