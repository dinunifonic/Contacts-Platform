package com.contacts.event;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ContactEventConsumerTest {

    private ContactEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ContactEventConsumer();
    }

    @Test
    void consumeFromKafka_shouldIgnoreDuplicateEvent() throws Exception {
        ContactCreatedEvent event = createEvent(7L);

        consumer.consumeFromKafka(event);
        consumer.consumeFromKafka(event);

        assertEquals(1, getProcessedEventCount());
    }

    @Test
    void consumeFromKafka_shouldProcessDifferentEventsForSameContact()
            throws Exception {
        ContactCreatedEvent firstEvent = createEvent(7L);
        ContactCreatedEvent secondEvent = createEvent(7L);

        consumer.consumeFromKafka(firstEvent);
        consumer.consumeFromKafka(secondEvent);

        assertEquals(2, getProcessedEventCount());
    }

    @Test
    void consume_shouldIgnoreDuplicateRabbitMQEvent() throws Exception {
        ContactCreatedEvent event = createEvent(7L);
        JsonObject payload = JsonObject.mapFrom(event);

        consumer.consume(payload);
        consumer.consume(payload);

        assertEquals(1, getProcessedEventCount());
    }

    @Test
    void consumeFromKafka_shouldNotThrowForDuplicateEvent() {
        ContactCreatedEvent event = createEvent(7L);

        assertDoesNotThrow(() -> {
            consumer.consumeFromKafka(event);
            consumer.consumeFromKafka(event);
        });
    }

    private ContactCreatedEvent createEvent(Long contactId) {
        return new ContactCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                contactId,
                Instant.now()
        );
    }

    private int getProcessedEventCount() throws Exception {
        Field field = ContactEventConsumer.class
                .getDeclaredField("processedEventCount");

        field.setAccessible(true);

        AtomicInteger count = (AtomicInteger) field.get(consumer);

        return count.get();
    }

    @Test
    void consumeFromRabbitMQAndKafka_shouldProcessSameEventOnlyOnce()
            throws Exception {
        ContactCreatedEvent event = createEvent(7L);
        JsonObject payload = JsonObject.mapFrom(event);

        consumer.consume(payload);          // First delivery: RabbitMQ
        consumer.consumeFromKafka(event);   // Duplicate delivery: Kafka

        assertEquals(1, getProcessedEventCount());
    }
}
