package com.blanksystem.blank.service.data.mapper;


import com.blanksystem.blank.service.data.entity.BlankEntity;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BlankDataAccessMapper {

    Blank blankEntityToBlank(BlankEntity blankEntity);

    @Mapping(target = "id", source = "blank.id.value")
    BlankEntity blankToBlankEntity(Blank blank);

    default BlankId map(UUID value) {
        return new BlankId(value);
    }
}
