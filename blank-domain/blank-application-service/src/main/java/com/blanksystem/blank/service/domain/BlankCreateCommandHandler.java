package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;
import com.blanksystem.blank.service.domain.exception.BlankDomainException;
import com.blanksystem.blank.service.domain.mapper.BlankDataMapper;
import com.blanksystem.blank.service.domain.ports.output.repository.BlankRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class BlankCreateCommandHandler {

    private final BlankDomainService blankDomainService;
    private final BlankRepository blankRepository;
    private final BlankDataMapper blankDataMapper;

    public BlankCreateCommandHandler(BlankDomainService blankDomainService,
                                     BlankRepository blankRepository,
                                     BlankDataMapper blankDataMapper) {
        this.blankDomainService = blankDomainService;
        this.blankRepository = blankRepository;
        this.blankDataMapper = blankDataMapper;
    }

    @Transactional
    public BlankCreatedEvent createBlank(CreateBlankCommand createBlankCommand) {
        final Blank blank = blankDataMapper.createBlankCommandToBlank(createBlankCommand);
        final BlankCreatedEvent blankCreatedEvent = blankDomainService.validateAndInitiateBlank(blank);
        final Blank savedBlank = blankRepository.createBlank(blank);
        if (savedBlank == null) {
            log.error("Could not save blank with id: {}", createBlankCommand.id());
            throw new BlankDomainException("Could not save blank with id "
                    + createBlankCommand.id());
        }
        log.info("Returning CustomerCreatedEvent for blank id: {}", createBlankCommand.id());
        return blankCreatedEvent;
    }

}
