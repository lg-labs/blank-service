package com.blanksystem.blank.service.data.mapper;


import com.blanksystem.blank.service.data.entity.BlankEntity;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.springframework.stereotype.Component;

@Component
public class BlankDataAccessMapper {

    public Blank blankEntityToBlank(BlankEntity blankEntity) {
        return new Blank(
                new BlankId(blankEntity.getId()));
    }

    public BlankEntity blankToBlankEntity(Blank customer) {
        return BlankEntity.builder()
                .id(customer.getId().getValue())
                .build();
    }


}
