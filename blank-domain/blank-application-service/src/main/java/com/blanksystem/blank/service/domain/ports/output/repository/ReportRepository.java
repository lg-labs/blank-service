package com.blanksystem.blank.service.domain.ports.output.repository;

import com.blanksystem.blank.service.domain.entity.Blank;

public interface ReportRepository {
    void save(Blank blank);
}
