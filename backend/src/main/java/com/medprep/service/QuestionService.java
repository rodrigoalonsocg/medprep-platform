package com.medprep.service;

import com.medprep.dto.request.CreateQuestionRequest;
import com.medprep.dto.request.SubmitAttemptRequest;
import com.medprep.dto.response.AttemptResponse;
import com.medprep.dto.response.QuestionResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.*;
import com.medprep.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserSpecialtyProgressRepository progressRepository;

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest req, UUID createdBy) {
        Specialty specialty = specialtyRepository.findById(req.getSpecialtyId())
                .orElseThrow(() -> MedPrepException.notFound("Especialidad", req.getSpecialtyId().toString()));

        Question question = Question.builder()
                .specialty(specialty)
                .stem(req.getStem())
                .optionA(req.getOptionA())
                .optionB(req.getOptionB())
                .optionC(req.getOptionC())
                .optionD(req.getOptionD())
                .optionE(req.getOptionE())
                .correctOption(req.getCorrectOption())
                .explanation(req.getExplanation())
                .difficulty(req.getDifficulty())
                .source(req.getSource())
                .year(req.getYear())
                .keywords(req.getKeywords())
                .createdBy(createdBy)
                .build();

        if (req.getSubspecialtyId() != null) {
            question.setSubspecialty(
                    specialty.getSubspecialties().stream()
                            .filter(s -> s.getId().equals(req.getSubspecialtyId()))
                            .findFirst()
                            .orElseThrow(() -> MedPrepException.notFound("Subespecialidad",
                                    req.getSubspecialtyId().toString()))
            );
        }

        return toResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        if (!questionRepository.existsById(id)) {
            throw MedPrepException.notFound("Pregunta", id.toString());
        }
        questionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findQuestions(UUID specialtyId, UUID subspecialtyId,
                                                 Question.Difficulty difficulty, String academy,
                                                 Pageable pageable) {
        return questionRepository
                .findWithFilters(specialtyId, subspecialtyId, difficulty, academy, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<String> listAcademies() {
        return questionRepository.findDistinctAcademies();
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findErrorQuestions(UUID userId, Pageable pageable) {
        Instant threeWeeksAgo = Instant.now().minus(21, ChronoUnit.DAYS);
        return questionRepository.findErrorQuestionsByUser(userId, threeWeeksAgo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> generateSimulacro(UUID specialtyId, int limit) {
        if (limit < 1 || limit > 100) throw MedPrepException.badRequest("El simulacro debe tener entre 1 y 100 preguntas");
        String specialtyIdStr = specialtyId != null ? specialtyId.toString() : null;
        return questionRepository.findRandomBySpecialty(specialtyIdStr, limit)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AttemptResponse submitAttempt(SubmitAttemptRequest req, UUID userId) {
        Question question = questionRepository.findById(req.getQuestionId())
                .orElseThrow(() -> MedPrepException.notFound("Pregunta", req.getQuestionId().toString()));

        boolean correct = question.getCorrectOption().equalsIgnoreCase(req.getSelectedOption());
        QuestionAttempt.AttemptStatus status = correct
                ? QuestionAttempt.AttemptStatus.CORRECTA
                : QuestionAttempt.AttemptStatus.INCORRECTA;

        QuestionAttempt attempt = QuestionAttempt.builder()
                .userId(userId)
                .question(question)
                .selectedOption(req.getSelectedOption().toUpperCase())
                .correct(correct)
                .timeSpentSeconds(req.getTimeSpentSeconds())
                .status(status)
                .build();

        attemptRepository.save(attempt);
        updateProgressAsync(userId, question.getSpecialty().getId());

        return AttemptResponse.builder()
                .id(attempt.getId())
                .questionId(question.getId())
                .selectedOption(attempt.getSelectedOption())
                .correctOption(question.getCorrectOption())
                .correct(correct)
                .status(status)
                .explanation(question.getExplanation())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }

    @Async
    public void updateProgressAsync(UUID userId, UUID specialtyId) {
        long total = attemptRepository.countTotalByUserAndSpecialty(userId, specialtyId);
        long correct = attemptRepository.countCorrectByUserAndSpecialty(userId, specialtyId);

        UserSpecialtyProgress progress = progressRepository
                .findByUserIdAndSpecialtyId(userId, specialtyId)
                .orElseGet(() -> {
                    Specialty spec = specialtyRepository.findById(specialtyId).orElseThrow();
                    return UserSpecialtyProgress.builder()
                            .userId(userId)
                            .specialty(spec)
                            .build();
                });

        progress.setTotalAttempts((int) total);
        progress.setCorrectAttempts((int) correct);
        progress.recalculate();
        progressRepository.save(progress);
    }

    private QuestionResponse toResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .specialtyId(q.getSpecialty().getId())
                .specialtyName(q.getSpecialty().getName())
                .subspecialtyId(q.getSubspecialty() != null ? q.getSubspecialty().getId() : null)
                .subspecialtyName(q.getSubspecialty() != null ? q.getSubspecialty().getName() : null)
                .stem(q.getStem())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .optionE(q.getOptionE())
                .explanation(q.getExplanation())
                .difficulty(q.getDifficulty())
                .source(q.getSource())
                .academy(q.getAcademy())
                .subsection(q.getSubsection())
                .year(q.getYear())
                .keywords(q.getKeywords())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
