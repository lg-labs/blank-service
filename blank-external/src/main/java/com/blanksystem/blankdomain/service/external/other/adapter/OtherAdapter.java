package com.blanksystem.blankdomain.service.external.other.adapter;

import com.blanksystem.blank.service.domain.ports.output.repository.OtherRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
public class OtherAdapter implements OtherRepository {

    public Optional<String> search(String search) {
        return Optional.empty();
    }
}
