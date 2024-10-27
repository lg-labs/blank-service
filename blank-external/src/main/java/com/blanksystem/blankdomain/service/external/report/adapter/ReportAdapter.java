package com.blanksystem.blankdomain.service.external.report.adapter;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blankdomain.service.external.report.client.ThirdSystemClient;
import com.blanksystem.blankdomain.service.external.report.mapper.ReportDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportAdapter implements ReportRepository {

    private final ThirdSystemClient client;
    private final ReportDataMapper reportDataMapper;


    public ReportAdapter(ThirdSystemClient client, ReportDataMapper reportDataMapper) {
        this.client = client;
        this.reportDataMapper = reportDataMapper;

    }

    @Override
    public void save(Blank blank) {

        log.info("Reporting a blank: {}", blank.getId().getValue());

        final var responseEntity = client.createUser(reportDataMapper.blankToUser(blank));

        log.info("Reported with status: {}", responseEntity.getStatusCode());
    }
}
