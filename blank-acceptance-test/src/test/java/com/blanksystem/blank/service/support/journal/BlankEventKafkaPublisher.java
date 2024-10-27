package com.blanksystem.blank.service.support.journal;

import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import com.blanksystem.blank.service.support.journal.config.BlankServiceConfigData;
import com.lg5.spring.kafka.producer.KafkaMessageHelper;
import com.lg5.spring.kafka.producer.service.KafkaProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class BlankEventKafkaPublisher {
    private final KafkaProducer<String, BlankAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final BlankServiceConfigData blankServiceConfigData;

    public void publish(BlankAvroModel blankAvroModel) {

        log.info("Received BlankCreatedEvent for blank id: {}", blankAvroModel.getId());
        try {

            kafkaProducer.send(
                    blankServiceConfigData.getTopic(),
                    blankAvroModel.getId(),
                    blankAvroModel,
                    kafkaMessageHelper.getCallback(blankAvroModel.getId(), blankAvroModel));
            log.info("BlankCreatedEvent sent to kafka for blank id: {} ", blankAvroModel.getId());
        } catch (Exception e) {
            log.error("Error while sending BlankCreatedEvent to kafka for blank id: {},"
                    + " error: {}", blankAvroModel.getId(), e.getMessage());
        }
    }
}
