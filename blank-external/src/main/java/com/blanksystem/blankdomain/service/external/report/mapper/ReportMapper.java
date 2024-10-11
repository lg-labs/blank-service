package com.blanksystem.blankdomain.service.external.report.mapper;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blankdomain.service.external.report.dto.User;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public User blankToUser(Blank blank) {
        return User.builder()
                .withUserId(blank.getId().getValue().toString())
                .build();
    }
}
