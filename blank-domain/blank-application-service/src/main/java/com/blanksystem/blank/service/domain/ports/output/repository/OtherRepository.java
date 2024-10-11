package com.blanksystem.blank.service.domain.ports.output.repository;

import java.util.Optional;

public interface OtherRepository {
    Optional<String> search(String search);
}
