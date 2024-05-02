package com.blanksystem.blank.service.api.rest;


import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankResponse;
import com.blanksystem.blank.service.domain.ports.input.service.BlankApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/blank", produces = "application/vnd.api.v1+json")
public class BlankController {

    private final BlankApplicationService blankApplicationService;

    public BlankController(BlankApplicationService blankApplicationService) {
        this.blankApplicationService = blankApplicationService;
    }


    @PostMapping
    public ResponseEntity<CreateBlankResponse> getOrderByTrackingId(@RequestBody CreateBlankCommand createBlankCommand) {
        log.info("Creating blank with id : {}", createBlankCommand.id());
        final CreateBlankResponse createBlankResponse = blankApplicationService.createBlank(createBlankCommand);
        log.info("Blank with id: {} was created", createBlankResponse.id());
        return ResponseEntity.ok(createBlankResponse);
    }

}
