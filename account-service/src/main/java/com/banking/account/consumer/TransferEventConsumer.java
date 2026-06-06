package com.banking.account.consumer;

import com.banking.account.dto.TransferEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransferEventConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @KafkaListener(topics = "transfer-events", groupId = "account-service-group")
    public void consume(String message) {
        try {
            TransferEventDto event = objectMapper.readValue(message, TransferEventDto.class);
            log.info("[AUDIT] Transfer event received — id={} status={} from={} to={} amount={}",
                    event.getId(), event.getStatus(),
                    event.getFromAccountId(), event.getToAccountId(), event.getAmount());
        } catch (Exception e) {
            log.error("Failed to process transfer event: {}", e.getMessage());
        }
    }
}
