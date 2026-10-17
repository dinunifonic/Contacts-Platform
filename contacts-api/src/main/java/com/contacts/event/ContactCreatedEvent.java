package com.contacts.event;

import java.time.Instant;
import java.util.UUID;

public class ContactCreatedEvent {
    private UUID eventId;
    private String correlationId;
    private Long contactId;
    private Instant occurredAt;

    public ContactCreatedEvent() {
    }

    public ContactCreatedEvent(UUID eventId, String correlationId, Long contactId, Instant occurredAt) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.contactId = contactId;
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getContactId() {
        return contactId;
    }
    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
