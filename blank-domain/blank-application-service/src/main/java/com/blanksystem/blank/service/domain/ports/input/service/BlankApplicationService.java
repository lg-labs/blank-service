package com.blanksystem.blank.service.domain.ports.input.service;

import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankResponse;
import jakarta.validation.Valid;

public interface BlankApplicationService {
    CreateBlankResponse createBlank(@Valid CreateBlankCommand createBlankCommand);
}
