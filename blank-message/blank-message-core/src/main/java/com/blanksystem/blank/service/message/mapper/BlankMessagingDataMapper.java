package com.blanksystem.blank.service.message.mapper;


import com.blanksystem.blank.service.domain.dto.message.BlankModel;
import com.blanksystem.blank.service.domain.event.BlankEvent;
import com.blanksystem.message.model.avro.BlankAvroModel;
import org.springframework.stereotype.Component;

@Component
public class BlankMessagingDataMapper {

    public BlankAvroModel customerCreatedEventToCustomerRequestAvroModel(BlankEvent blankEvent) {
        return BlankAvroModel.newBuilder()
                .setId(blankEvent.getBlank().getId().getValue().toString())
                .build();
    }

    public BlankModel blankAvroModelToBlankModel(BlankAvroModel blankAvroModel) {
        return BlankModel.builder()
                .id(blankAvroModel.getId())
                .build();
    }
}
