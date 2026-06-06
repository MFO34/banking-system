package com.banking.transfer.service;

import com.banking.transfer.entity.OutboxEvent;
import com.banking.transfer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {

    private static final String TOPIC = "transfer-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : unpublished) {
            kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), event.getPayload());
            event.markPublished();
            outboxEventRepository.save(event);
            log.info("Published outbox event: {} for transfer {}", event.getEventType(), event.getAggregateId());
        }
    }
}
