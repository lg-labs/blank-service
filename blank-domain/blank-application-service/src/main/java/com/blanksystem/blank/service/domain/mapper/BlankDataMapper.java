package com.blanksystem.blank.service.domain.mapper;

import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankResponse;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BlankDataMapper {

    Blank createBlankCommandToBlank(CreateBlankCommand createBlankCommand);

    @Mapping(target = "id", source = "blank.id.value")
    @Mapping(target = "message", source = "message")
    CreateBlankResponse blankToCreateBlankResponse(Blank blank, String message);

    default BlankId map(UUID value) {
        return new BlankId(value);
    }
}
