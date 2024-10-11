package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.dto.message.BlankModel;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.BlankRepository;
import com.blanksystem.blank.service.domain.ports.output.repository.OtherRepository;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BlankMessageListenerImplTest {

    private BlankRepository repository;
    private ReportRepository reportRepository;
    private OtherRepository otherRepository;
    private BlankMessageListenerImpl sut;

    @BeforeEach
    void setUp() {
        repository = mock(BlankRepository.class);
        reportRepository = mock(ReportRepository.class);
        otherRepository = mock(OtherRepository.class);
        sut = new BlankMessageListenerImpl(repository, reportRepository, otherRepository);

    }

    @ParameterizedTest
    @MethodSource("com.blanksystem.blank.service.domain.support.BlankModelMother#givenABlankModel")
    void blankCreated(BlankModel givenABlankModel) {
        // given
        final var blank = new Blank(new BlankId(UUID.fromString(givenABlankModel.getId())));
        given(repository.findbyId(UUID.fromString(givenABlankModel.getId())))
                .willReturn(Optional.of(blank));

        sut.blankCreated(givenABlankModel);

        verify(otherRepository).search(anyString());
        verify(repository).findbyId(UUID.fromString(givenABlankModel.getId()));
        verify(reportRepository).save(blank);
    }
}