package com.blanksystem.blankdomain.service.external.report.adapter;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blankdomain.service.external.report.client.ThirdSystemClient;
import com.blanksystem.blankdomain.service.external.report.mapper.ReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportAdapter implements ReportRepository {

    private final ThirdSystemClient client;
    private final ReportMapper reportMapper;


    public ReportAdapter(ThirdSystemClient client, ReportMapper reportMapper) {
        this.client = client;
        this.reportMapper = reportMapper;

    }

    @Override
    public void save(Blank blank) {

        log.info("Reporting a blank: {}", blank.getId().getValue());

        final var responseEntity = client.createUser(reportMapper.blankToUser(blank));

        log.info("Reported with status: {}", responseEntity.getStatusCode());
    }
}
