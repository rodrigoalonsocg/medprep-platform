package com.medprep.service;

import com.medprep.dto.request.CreateStudySessionRequest;
import com.medprep.dto.response.StudySessionResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.Specialty;
import com.medprep.model.StudySession;
import com.medprep.repository.SpecialtyRepository;
import com.medprep.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    @Mock
    StudySessionRepository sessionRepository;
    @Mock
    SpecialtyRepository specialtyRepository;
    @InjectMocks
    StudySessionService service;

    @Test
    void create_sinEspecialidad_guardaSesion() {
        UUID userId = UUID.randomUUID();
        CreateStudySessionRequest req = new CreateStudySessionRequest();
        req.setDurationMinutes(50);
        req.setSessionType(StudySession.SessionType.POMODORO);

        when(sessionRepository.save(any(StudySession.class))).thenAnswer(i -> i.getArgument(0));

        StudySessionResponse res = service.create(req, userId);

        assertThat(res.getDurationMinutes()).isEqualTo(50);
        assertThat(res.getSessionType()).isEqualTo(StudySession.SessionType.POMODORO);
        assertThat(res.getSpecialtyId()).isNull();
        assertThat(res.getEndedAt()).isNotNull();
    }

    @Test
    void create_conEspecialidadInexistente_lanzaNotFound() {
        UUID specialtyId = UUID.randomUUID();
        CreateStudySessionRequest req = new CreateStudySessionRequest();
        req.setDurationMinutes(50);
        req.setSessionType(StudySession.SessionType.LIBRE);
        req.setSpecialtyId(specialtyId);

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req, UUID.randomUUID()))
                .isInstanceOf(MedPrepException.class);
    }

    @Test
    void create_conEspecialidad_mapeaNombre() {
        UUID specialtyId = UUID.randomUUID();
        Specialty specialty = Specialty.builder().id(specialtyId).name("Pediatría").code("PEDIAT").build();

        CreateStudySessionRequest req = new CreateStudySessionRequest();
        req.setDurationMinutes(25);
        req.setSessionType(StudySession.SessionType.POMODORO);
        req.setSpecialtyId(specialtyId);

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(sessionRepository.save(any(StudySession.class))).thenAnswer(i -> i.getArgument(0));

        StudySessionResponse res = service.create(req, UUID.randomUUID());

        assertThat(res.getSpecialtyId()).isEqualTo(specialtyId);
        assertThat(res.getSpecialtyName()).isEqualTo("Pediatría");
    }
}
