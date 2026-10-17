package com.contacts.event;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ContactEventPublisher {
    private static final Logger LOG = Logger.getLogger(ContactEventPublisher.class);

    @Channel("contact-created-out")
    Emitter<ContactCreatedEvent> contactCreatedEmitter;

    public CompletionStage<Void> publish(ContactCreatedEvent event) {
        return contactCreatedEmitter.send(event)
                .whenComplete((ignored, failure) -> {
                    if (failure == null) {
                        LOG.infof(
                                "Contact event sent: eventId=%s, contactId=%s",
                                event.getEventId(),
                                event.getContactId()
                        );
                    } else {
                        LOG.errorf(
                                failure,
                                "Failed to send contact event: eventId=%s",
                                event.getEventId()
                        );
                    }
                });
    }

}
