package com.blanksystem.blank.service.domain.mapper;

import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankResponse;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.springframework.stereotype.Component;

@Component
public class BlankDataMapper {
    public Blank createBlankCommandToBlank(CreateBlankCommand createBlankCommand) {
        return new Blank(
                new BlankId(createBlankCommand.id())
        );
    }

    public CreateBlankResponse blankToCreateBlankResponse(Blank blank, String message) {
        return new CreateBlankResponse(blank.getId().getValue(), message);
    }
}
