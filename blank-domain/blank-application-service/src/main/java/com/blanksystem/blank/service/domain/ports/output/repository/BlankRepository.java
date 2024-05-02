package com.blanksystem.blank.service.domain.ports.output.repository;

import com.blanksystem.blank.service.domain.entity.Blank;

import java.util.Optional;
import java.util.UUID;

public interface BlankRepository {

    Blank createBlank(Blank blank);

    Optional<Blank> findbyId(UUID blankId);
}
