package com.medprep.service;

import com.medprep.dto.request.CreateStudySessionRequest;
import com.medprep.dto.response.StudySessionResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.Specialty;
import com.medprep.model.StudySession;
import com.medprep.repository.SpecialtyRepository;
import com.medprep.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final SpecialtyRepository specialtyRepository;

    @Transactional
    public StudySessionResponse create(CreateStudySessionRequest req, UUID userId) {
        StudySession session = StudySession.builder()
                .userId(userId)
                .durationMinutes(req.getDurationMinutes())
                .sessionType(req.getSessionType())
                .startedAt(req.getStartedAt())
                .endedAt(req.getEndedAt() != null ? req.getEndedAt() : Instant.now())
                .build();

        if (req.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(req.getSpecialtyId())
                    .orElseThrow(() -> MedPrepException.notFound("Especialidad", req.getSpecialtyId().toString()));
            session.setSpecialty(specialty);
        }

        return toResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> listForUser(UUID userId) {
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long minutesThisWeek(UUID userId) {
        return sessionRepository.sumMinutesSince(userId, Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private StudySessionResponse toResponse(StudySession s) {
        return StudySessionResponse.builder()
                .id(s.getId())
                .specialtyId(s.getSpecialty() != null ? s.getSpecialty().getId() : null)
                .specialtyName(s.getSpecialty() != null ? s.getSpecialty().getName() : null)
                .durationMinutes(s.getDurationMinutes())
                .sessionType(s.getSessionType())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .build();
    }
}
