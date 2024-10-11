package com.blanksystem.blank.service.domain.support;

import com.blanksystem.blank.service.domain.dto.message.BlankModel;

import java.util.UUID;
import java.util.stream.Stream;

public class BlankModelMother {

    public static Stream<BlankModel> givenABlankModel() {
        return Stream.of(BlankModel.builder().id(UUID.randomUUID().toString()).build());
    }
}
