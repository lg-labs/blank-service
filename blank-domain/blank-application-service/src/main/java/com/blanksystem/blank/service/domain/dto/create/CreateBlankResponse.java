package com.blanksystem.blank.service.domain.dto.create;


import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBlankResponse(@NotNull UUID id,
                                  @NotNull String message) {

}
