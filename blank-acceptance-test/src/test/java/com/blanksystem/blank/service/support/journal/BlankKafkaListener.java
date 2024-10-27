package com.blanksystem.blank.service.support.journal;

import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import com.blanksystem.blank.service.support.world.BlankWorld;
import com.lg5.spring.kafka.consumer.KafkaConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
public class BlankKafkaListener implements KafkaConsumer<BlankAvroModel> {

    private final BlankWorld blankWorld;

    @Override
    @KafkaListener(
            id = "${blanksystem.blank.events.journal.blank.consumer.group}",
            topics = "${blanksystem.blank.events.journal.blank.topic}"
    )
    public void receive(@Payload List<BlankAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of blank create messages received with keys {}, partitions {} and offsets {}"
                        + ", sending for blank",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());
        messages.forEach(blankAvroModel -> {
            try {
                log.info("Processing blank created for id: {}", blankAvroModel.getId());
                blankWorld.getMessages().add(blankAvroModel);

            } catch (Exception e) {
                throw new RuntimeException("Throwing DataAccessException in"
                        + " BlankKafkaListener: " + e.getMessage(), e);
            }
        });
    }
}
