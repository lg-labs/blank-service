package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankResponse;
import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;
import com.blanksystem.blank.service.domain.mapper.BlankDataMapper;
import com.blanksystem.blank.service.domain.ports.input.service.BlankApplicationService;
import com.blanksystem.blank.service.domain.ports.output.message.publisher.BlankMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@Service
class BlankApplicationServiceImpl implements BlankApplicationService {

    private final BlankCreateCommandHandler blankCreateCommandHandler;
    private final BlankDataMapper blankDataMapper;
    private final BlankMessagePublisher blankMessagePublisher;

    public BlankApplicationServiceImpl(BlankCreateCommandHandler blankCreateCommandHandler,
                                       BlankDataMapper blankDataMapper,
                                       BlankMessagePublisher blankMessagePublisher) {
        this.blankCreateCommandHandler = blankCreateCommandHandler;
        this.blankDataMapper = blankDataMapper;
        this.blankMessagePublisher = blankMessagePublisher;
    }

    /**
     * TODO:
     * still need to outbox pattern because, after persisting the data(blank)
     * into database, i cannot be sure if the publish operation will be successful or if it is not successful.
     * <p>
     * I will have inconsistency😳, between the blank tables in blank service and others services.
     *
     * @param createBlankCommand for create a blank
     * @return status final, after that processed and sent event
     */
    @Override
    public CreateBlankResponse createBlank(CreateBlankCommand createBlankCommand) {
        final BlankCreatedEvent blankCreatedEvent = blankCreateCommandHandler.createBlank(createBlankCommand);
        blankMessagePublisher.publish(blankCreatedEvent);
        return blankDataMapper
                .blankToCreateBlankResponse(blankCreatedEvent.getBlank(),
                        "Customer saved successfully!");
    }
}
