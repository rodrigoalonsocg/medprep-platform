package com.medprep.service;

import com.medprep.ai.adapter.AiAdapter;
import com.medprep.dto.response.FlashcardResponse;
import com.medprep.dto.response.PatternResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.ClinicalPattern;
import com.medprep.model.Flashcard;
import com.medprep.model.Question;
import com.medprep.repository.ClinicalPatternRepository;
import com.medprep.repository.FlashcardRepository;
import com.medprep.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiAdapter aiAdapter;
    private final QuestionRepository questionRepository;
    private final ClinicalPatternRepository patternRepository;
    private final FlashcardRepository flashcardRepository;

    @Transactional
    public CompletableFuture<PatternResponse> analyzePattern(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> MedPrepException.notFound("Pregunta", questionId.toString()));

        // Si ya existe el patrón, lo retorna sin llamar a la IA
        return patternRepository.findByQuestionId(questionId)
                .map(existing -> CompletableFuture.completedFuture(toPatternResponse(existing, questionId)))
                .orElseGet(() -> aiAdapter.analyzePattern(question.getStem())
                        .thenApply(result -> {
                            ClinicalPattern pattern = ClinicalPattern.builder()
                                    .question(question)
                                    .keywords(result.keywords())
                                    .diagnosis(result.diagnosis())
                                    .pearl(result.pearl())
                                    .distractors(result.distractors())
                                    .build();
                            patternRepository.save(pattern);

                            // Actualiza keywords en la pregunta también
                            question.setKeywords(result.keywords());
                            questionRepository.save(question);

                            return toPatternResponse(pattern, questionId);
                        }));
    }

    @Transactional
    public CompletableFuture<List<FlashcardResponse>> generateFlashcards(UUID questionId, UUID userId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> MedPrepException.notFound("Pregunta", questionId.toString()));

        String correctAnswer = switch (question.getCorrectOption()) {
            case "A" -> question.getOptionA();
            case "B" -> question.getOptionB();
            case "C" -> question.getOptionC();
            case "D" -> question.getOptionD();
            case "E" -> question.getOptionE();
            default -> "";
        };

        return aiAdapter.generateFlashcards(question.getStem(), correctAnswer, question.getExplanation())
                .thenApply(results -> results.stream().map(fc -> {
                    Flashcard flashcard = Flashcard.builder()
                            .userId(userId)
                            .question(question)
                            .front(fc.front())
                            .back(fc.back())
                            .specialty(question.getSpecialty())
                            .build();
                    flashcardRepository.save(flashcard);
                    return toFlashcardResponse(flashcard);
                }).toList());
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getUserFlashcards(UUID userId) {
        return flashcardRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toFlashcardResponse).toList();
    }

    @Transactional
    public String exportToAnki(UUID userId) {
        List<Flashcard> pending = flashcardRepository.findByUserIdAndExportedFalse(userId);
        String content = pending.stream()
                .map(f -> f.getFront() + ";" + f.getBack())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        pending.forEach(f -> f.setExported(true));
        flashcardRepository.saveAll(pending);

        return content;
    }

    private PatternResponse toPatternResponse(ClinicalPattern p, UUID questionId) {
        return PatternResponse.builder()
                .questionId(questionId)
                .keywords(p.getKeywords())
                .diagnosis(p.getDiagnosis())
                .pearl(p.getPearl())
                .distractors(p.getDistractors())
                .build();
    }

    private FlashcardResponse toFlashcardResponse(Flashcard f) {
        return FlashcardResponse.builder()
                .id(f.getId())
                .questionId(f.getQuestion() != null ? f.getQuestion().getId() : null)
                .front(f.getFront())
                .back(f.getBack())
                .specialtyName(f.getSpecialty() != null ? f.getSpecialty().getName() : null)
                .exported(f.isExported())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
