package com.blanksystem.blank.service.message.publisher.kafka;


import com.blanksystem.blank.service.domain.config.BlankServiceConfigData;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;
import com.blanksystem.blank.service.domain.ports.output.message.publisher.BlankMessagePublisher;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import com.blanksystem.blank.service.message.mapper.BlankMessagingDataMapper;
import com.blanksystem.message.model.avro.BlankAvroModel;
import com.lg5.spring.kafka.producer.KafkaMessageHelper;
import com.lg5.spring.kafka.producer.service.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BlankEventKafkaPublisher implements BlankMessagePublisher {

    private final BlankMessagingDataMapper customerMessagingDataMapper;
    private final KafkaProducer<String, BlankAvroModel> kafkaProducer;
    private final BlankServiceConfigData customerServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public BlankEventKafkaPublisher(BlankMessagingDataMapper customerMessagingDataMapper,
                                    KafkaProducer<String, BlankAvroModel> kafkaProducer,
                                    BlankServiceConfigData customerServiceConfigData, KafkaMessageHelper kafkaMessageHelper) {
        this.customerMessagingDataMapper = customerMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.customerServiceConfigData = customerServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(BlankCreatedEvent blankCreatedEvent) {
        final Blank blank = blankCreatedEvent.getBlank();
        final BlankId blankId = blank.getId();
        log.info("Received BlankCreatedEvent for blank id: {}", blankId.getValue());


        try {
            final BlankAvroModel blankAvroModel =
                    customerMessagingDataMapper.customerCreatedEventToCustomerRequestAvroModel(blankCreatedEvent);

            kafkaProducer.send(
                    customerServiceConfigData.getBlankTopicName(),
                    blankAvroModel.getId(),
                    blankAvroModel,
                    kafkaMessageHelper.getCallback(blankAvroModel.getId(), blankAvroModel));


            log.info("BlankCreatedEvent sent to kafka for blank id: {} ", blankAvroModel.getId());
        } catch (Exception e) {
            log.error("Error while sending BlankCreatedEvent to kafka for blank id: {},"
                    + " error: {}", blank.getId().getValue(), e.getMessage());
        }
    }
}
