package com.contacts.event;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ContactCreatedEventConsumer {

    private static final Logger LOG = Logger.getLogger(ContactCreatedEventConsumer.class);

    @Incoming("contact-created-in")
    public void consume(JsonObject payload) {
        ContactCreatedEvent event = payload.mapTo(ContactCreatedEvent.class);

        int maxAttempts = 4;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                processEvent(event);
                return; // success: stop retrying
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
    private void processEvent(ContactCreatedEvent event) {
        LOG.infof(
                "Received ContactCreatedEvent: eventId=%s, correlationId=%s, contactId=%s, occurredAt=%s",
                event.getEventId(),
                event.getCorrelationId(),
                event.getContactId(),
                event.getOccurredAt()
        );
    }
}