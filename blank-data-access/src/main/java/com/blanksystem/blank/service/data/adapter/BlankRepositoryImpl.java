package com.blanksystem.blank.service.data.adapter;

import com.blanksystem.blank.service.data.mapper.BlankDataAccessMapper;
import com.blanksystem.blank.service.data.repository.BlankJPARepository;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.BlankRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class BlankRepositoryImpl implements BlankRepository {
    private final BlankJPARepository repository;
    private final BlankDataAccessMapper blankDataAccessMapper;


    public BlankRepositoryImpl(BlankJPARepository repository, BlankDataAccessMapper blankDataAccessMapper) {
        this.repository = repository;
        this.blankDataAccessMapper = blankDataAccessMapper;
    }

    @Override
    public Blank createBlank(Blank blank) {
        return blankDataAccessMapper.blankEntityToBlank(
                repository.save(blankDataAccessMapper.blankToBlankEntity(blank)));
    }

    @Override
    public Optional<Blank> findbyId(UUID blankId) {
        return repository.findById(blankId)
                .map(blankDataAccessMapper::blankEntityToBlank);
    }
}
