package com.contacts.event;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class ContactEventPublisher {

    private static final Logger LOG = Logger.getLogger(ContactEventPublisher.class);

    @Channel("contact-created-out")
    Emitter<ContactCreatedEvent> contactCreatedEmitter;

    @Channel("contact-created-kafka-out")
    Emitter<ContactCreatedEvent> contactCreatedKafkaEmitter;

    public CompletionStage<Void> publish(ContactCreatedEvent event) {
        CompletionStage<Void> rabbitResult = publishToRabbitMQ(event);
        CompletionStage<Void> kafkaResult = publishToKafka(event);

        return CompletableFuture.allOf(
                rabbitResult.toCompletableFuture(),
                kafkaResult.toCompletableFuture()
        ).whenComplete((ignored, failure) -> {
            if (failure == null) {
                LOG.infof(
                        "Contact event sent to RabbitMQ and Kafka: eventId=%s, correlationId=%s, contactId=%s",
                        event.getEventId(),
                        event.getCorrelationId(),
                        event.getContactId()
                );
            } else {
                LOG.errorf(
                        failure,
                        "Failed to publish contact event: eventId=%s",
                        event.getEventId()
                );
            }
        });
    }

    private CompletionStage<Void> publishToRabbitMQ(
            ContactCreatedEvent event) {
        return contactCreatedEmitter.send(event);
    }

    private CompletionStage<Void> publishToKafka(
            ContactCreatedEvent event) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        Message<ContactCreatedEvent> message = Message.of(event)
                .addMetadata(
                        OutgoingKafkaRecordMetadata.<String>builder()
                                .withKey(event.getContactId().toString())
                                .build()
                )
                .withAck(() -> {
                    result.complete(null);
                    return CompletableFuture.completedFuture(null);
                })
                .withNack(failure -> {
                    result.completeExceptionally(failure);
                    return CompletableFuture.completedFuture(null);
                });

        try {
            contactCreatedKafkaEmitter.send(message);
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
        }

        return result;
    }
}
