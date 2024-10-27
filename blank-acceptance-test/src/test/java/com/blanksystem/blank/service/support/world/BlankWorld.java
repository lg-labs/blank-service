package com.blanksystem.blank.service.support.world;

import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import com.blanksystem.blank.service.support.dto.CreateBlankCommand;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
public class BlankWorld {

    private CreateBlankCommand createBlankCommand;
    private Response response;
    private List<BlankAvroModel> messages = new ArrayList<>();

}
