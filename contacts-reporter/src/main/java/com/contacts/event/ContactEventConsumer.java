package com.contacts.event;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ContactEventConsumer {
    private static final Logger LOG = Logger.getLogger(ContactEventConsumer.class);

    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    private final AtomicInteger processedEventCount = new AtomicInteger(0);

    @Incoming("contact-created-in")
    public void consume(JsonObject payload) {
        ContactCreatedEvent event = payload.mapTo(ContactCreatedEvent.class);

        int maxAttempts = 4;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                processEvent(event, "RabbitMQ");
                return;
            } catch (RuntimeException e) {
                LOG.warnf(
                        "Processing failed: attempt=%d/%d, eventId=%s, error=%s",
                        attempt,
                        maxAttempts,
                        event.getEventId(),
                        e.getMessage()
                );

                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
    }
    private void processEvent(ContactCreatedEvent event, String source) {
        UUID eventId = event.getEventId();

        if (!processedEventIds.add(eventId)) {
            LOG.infof(
                    "Duplicate event ignored: source=%s, eventId=%s",
                    source,
                    eventId
            );
            return;
        }
        int count = processedEventCount.incrementAndGet();

        LOG.infof(
                "Received ContactCreatedEvent from %s: eventId=%s, correlationId=%s, contactId=%s, processedCount=%d",
                source,
                event.getEventId(),
                event.getCorrelationId(),
                event.getContactId(),
                count
        );

        processedEventIds.add(eventId);
    }

    @Incoming("contact-created-kafka-in")
    public void consumeFromKafka(ContactCreatedEvent event) {
        processEvent(event, "Kafka");
    }
}