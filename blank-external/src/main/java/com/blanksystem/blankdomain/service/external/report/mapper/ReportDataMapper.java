package com.blanksystem.blankdomain.service.external.report.mapper;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blankdomain.service.external.report.dto.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReportDataMapper {
    ReportDataMapper INSTANCE = Mappers.getMapper(ReportDataMapper.class);

    @Mapping(target = "userId", expression = "java(blank.getId().getValue().toString())")
    User blankToUser(Blank blank);
}
