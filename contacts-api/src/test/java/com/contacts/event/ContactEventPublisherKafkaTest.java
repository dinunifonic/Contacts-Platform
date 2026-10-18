package com.contacts.event;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@WithTestResource(KafkaCompanionResource.class)
public class ContactEventPublisherKafkaTest {
    @Inject
    ContactEventPublisher contactEventPublisher;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    void publish_shouldDeliverEventsOfTheSameContactToTheSamePartition() throws Exception {
        ConsumerTask<String, String> consumer = companion.consumeStrings().fromTopics("contacts.created");

        publishAndWait(event(700001L));
        publishAndWait(event(700001L));
        publishAndWait(event(700001L));

        List<ConsumerRecord<String, String>> records = awaitRecords(consumer, "700001");
        assertThat(records).extracting(ConsumerRecord::partition).containsOnly(records.get(0).partition());
    }

    @Test
    void publish_shouldDeliverEventsOfTheSameContactInPublishOrder() throws Exception {
        ContactCreatedEvent first = event(700002L);
        ContactCreatedEvent second = event(700002L);
        ContactCreatedEvent third = event(700002L);
        ConsumerTask<String, String> consumer = companion.consumeStrings().fromTopics("contacts.created");

        publishAndWait(first);
        publishAndWait(second);
        publishAndWait(third);

        List<ConsumerRecord<String, String>> records = awaitRecords(consumer, "700002");
        assertThat(records)
                .extracting(record -> new JsonObject(record.value()).getString("eventId"))
                .containsExactly(first.getEventId().toString(), second.getEventId().toString(), third.getEventId().toString());
    }

    private ContactCreatedEvent event(Long contactId) {
        return new ContactCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString(), contactId, Instant.now());
    }

    private void publishAndWait(ContactCreatedEvent event) throws Exception {
        contactEventPublisher.publish(event).toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private List<ConsumerRecord<String, String>> awaitRecords(ConsumerTask<String, String> consumer, String key) {
        await().atMost(Duration.ofSeconds(30)).until(() -> recordsWithKey(consumer, key).size() == 3);
        return recordsWithKey(consumer, key);
    }

    private List<ConsumerRecord<String, String>> recordsWithKey(ConsumerTask<String, String> consumer, String key) {
        return consumer.getRecords().stream().filter(record -> key.equals(record.key())).toList();
    }
}
