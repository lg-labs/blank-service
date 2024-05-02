package com.blanksystem.blank.service.domain.ports.input.message.listener.blank;


import com.blanksystem.blank.service.domain.dto.message.BlankModel;
import org.springframework.stereotype.Service;

@Service
public interface BlankMessageListener {
    void blankCreated(BlankModel blankModel);
}
