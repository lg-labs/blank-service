package com.blanksystem.blank.service.message.mapper;


import com.blanksystem.blank.service.domain.dto.message.BlankModel;
import com.blanksystem.blank.service.domain.event.BlankEvent;
import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlankMessagingDataMapper {

    @Mapping(target = "id", source = "blankEvent.blank.id.value")
    BlankAvroModel customerCreatedEventToCustomerRequestAvroModel(BlankEvent blankEvent);

    BlankModel blankAvroModelToBlankModel(BlankAvroModel blankAvroModel);
}
