package com.blanksystem.blank.service.domain.dto.create;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;


@Builder
public record CreateBlankCommand(@NotNull UUID id) {

}
