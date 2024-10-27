package com.blanksystem.blank.service.support.world;

import com.blanksystem.blank.service.support.data.BlankJPARepository;
import io.cucumber.java.After;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@AllArgsConstructor
public class BlankWorldHooks {

    private final BlankWorld blankWorld;
    private final BlankJPARepository blankJPARepository;

    @After
    public void cleanUp() {
        log.info("Clean up");
        blankWorld.setCreateBlankCommand(null);
        blankWorld.setResponse(null);
        blankWorld.setMessages(new ArrayList<>());
        blankJPARepository.deleteAll();
        log.info("Clean up complete");
    }
}
