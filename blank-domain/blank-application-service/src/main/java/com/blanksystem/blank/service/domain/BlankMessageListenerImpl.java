package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.dto.message.BlankModel;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.input.message.listener.blank.BlankMessageListener;
import com.blanksystem.blank.service.domain.ports.output.repository.BlankRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.util.function.Consumer;


/**
 * <h2>SAGA Rollback Operation</h2>
 * Because of a business logic invariant but it can be a response part of the  SAGA ROLLBACK operation.
 */
@Slf4j
@Validated
@Service
public class BlankMessageListenerImpl implements BlankMessageListener {
    private final BlankRepository repository;

    public BlankMessageListenerImpl(BlankRepository repository) {
        this.repository = repository;
    }

    @Override
    public void blankCreated(BlankModel blankModel) {
        repository.findbyId(UUID.fromString(blankModel.getId()))
                .ifPresent(showConfirmation());
    }

    private Consumer<Blank> showConfirmation() {
        return blank -> log.info("Confirmation the creation to blank with id: {}", blank.getId().getValue());
    }
}
