package com.contacts.event;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class ContactEventPublisherTest {

    @Mock
    Emitter<ContactCreatedEvent> contactCreatedEmitter;

    @Mock
    Emitter<ContactCreatedEvent> contactCreatedKafkaEmitter;

    @InjectMocks
    ContactEventPublisher contactEventPublisher;

    @BeforeEach
    void setUp() {
        when(contactCreatedEmitter.send(any(ContactCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publish_shouldSendEventToRabbitMQAndKafka() {
        ContactCreatedEvent event = event(7L);

        contactEventPublisher.publish(event);

        verify(contactCreatedEmitter).send(event);
        verify(contactCreatedKafkaEmitter).send(any(Message.class));
    }

    @Test
    void publish_shouldUseContactIdAsKafkaKey() {
        ContactCreatedEvent first = event(7L);
        ContactCreatedEvent second = event(7L);

        contactEventPublisher.publish(first);
        contactEventPublisher.publish(second);

        ArgumentCaptor<Message<ContactCreatedEvent>> captor =
                ArgumentCaptor.forClass(Message.class);

        verify(contactCreatedKafkaEmitter, times(2)).send(captor.capture());

        List<Message<ContactCreatedEvent>> messages = captor.getAllValues();

        String firstKey = messages.get(0)
                .getMetadata(OutgoingKafkaRecordMetadata.class)
                .map(metadata -> (String) metadata.getKey())
                .orElseThrow();

        String secondKey = messages.get(1)
                .getMetadata(OutgoingKafkaRecordMetadata.class)
                .map(metadata -> (String) metadata.getKey())
                .orElseThrow();

        assertEquals("7", firstKey);
        assertEquals(firstKey, secondKey);
    }

    private ContactCreatedEvent event(Long contactId) {
        return new ContactCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                contactId,
                Instant.now()
        );
    }
}
