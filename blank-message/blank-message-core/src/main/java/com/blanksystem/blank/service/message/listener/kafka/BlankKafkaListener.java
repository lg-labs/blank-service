package com.blanksystem.blank.service.message.listener.kafka;


import com.blanksystem.blank.service.domain.exception.BlankApplicationServiceException;
import com.blanksystem.blank.service.domain.ports.input.message.listener.blank.BlankMessageListener;
import com.blanksystem.blank.service.message.mapper.BlankMessagingDataMapper;
import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import com.lg5.spring.kafka.consumer.KafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BlankKafkaListener implements KafkaConsumer<BlankAvroModel> {

    private final BlankMessageListener blankMessageListener;
    private final BlankMessagingDataMapper mapper;

    public BlankKafkaListener(BlankMessageListener blankMessageListener, BlankMessagingDataMapper mapper) {
        this.blankMessageListener = blankMessageListener;
        this.mapper = mapper;
    }

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
                blankMessageListener.blankCreated(mapper
                        .blankAvroModelToBlankModel(blankAvroModel));

            } catch (Exception e) {
                throw new BlankApplicationServiceException("Throwing DataAccessException in"
                        + " BlankKafkaListener: " + e.getMessage(), e);
            }
        });
    }
}
