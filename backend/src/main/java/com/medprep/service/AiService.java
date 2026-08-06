package com.medprep.service;

import com.medprep.ai.adapter.AiAdapter;
import com.medprep.dto.response.ExamImportResponse;
import com.medprep.dto.response.FlashcardResponse;
import com.medprep.dto.response.PatternResponse;
import com.medprep.dto.response.QuestionResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.ClinicalPattern;
import com.medprep.model.Flashcard;
import com.medprep.model.Question;
import com.medprep.model.Specialty;
import com.medprep.repository.ClinicalPatternRepository;
import com.medprep.repository.FlashcardRepository;
import com.medprep.repository.QuestionRepository;
import com.medprep.repository.SpecialtyRepository;
import com.medprep.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SpecialtyRepository specialtyRepository;
    private final UserProfileRepository userProfileRepository;

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

    /**
     * Importa un examen: la IA detecta y clasifica cada pregunta por especialidad,
     * y se guardan en el banco. Devuelve las preguntas creadas.
     */
    @Transactional
    public ExamImportResponse importFromExam(String examText, UUID adminId, String academy) {
        List<Specialty> specialties = specialtyRepository.findAll();
        if (specialties.isEmpty()) {
            throw MedPrepException.badRequest("No hay especialidades configuradas en el sistema.");
        }
        List<String> names = specialties.stream().map(Specialty::getName).toList();

        // Trocea el examen en lotes y los procesa EN PARALELO: evita que la
        // respuesta de la IA se trunque (exámenes grandes) y es mucho más rápido.
        List<String> batches = splitIntoBatches(examText, 5);
        // Procesa en grupos de 3 llamadas concurrentes para no agotar el rate limit de la IA.
        List<AiAdapter.ExtractedQuestion> extracted = new ArrayList<>();
        int concurrency = 3;
        for (int i = 0; i < batches.size(); i += concurrency) {
            List<String> group = batches.subList(i, Math.min(i + concurrency, batches.size()));
            group.stream()
                    .map(batch -> aiAdapter.extractQuestions(batch, names)
                            .exceptionally(ex -> {
                                log.warn("Lote falló, se omite: {}", ex.getMessage());
                                return List.of();
                            }))
                    .toList()                          // dispara el grupo en paralelo
                    .forEach(f -> extracted.addAll(f.join()));
        }
        // Deduplica por enunciado (el solape entre lotes puede repetir preguntas).
        Map<String, AiAdapter.ExtractedQuestion> uniq = new LinkedHashMap<>();
        for (AiAdapter.ExtractedQuestion eq : extracted) {
            if (eq.stem() == null || eq.stem().isBlank()) continue;
            uniq.putIfAbsent(stemKey(eq.stem()), eq);
        }
        if (uniq.isEmpty()) {
            throw MedPrepException.badRequest(
                    "La IA no detectó preguntas en el PDF. Verifica que el examen tenga preguntas de opción múltiple con sus respuestas.");
        }
        // Evita duplicar preguntas que YA existen de la misma academia (ediciones repetidas).
        Set<String> existingKeys = questionRepository.findStemsByAcademy(academy).stream()
                .filter(java.util.Objects::nonNull).map(AiService::stemKey).collect(java.util.stream.Collectors.toSet());
        List<AiAdapter.ExtractedQuestion> questions = uniq.values().stream()
                .filter(eq -> !existingKeys.contains(stemKey(eq.stem())))
                .collect(java.util.stream.Collectors.toList());

        Map<String, Specialty> byName = new HashMap<>();
        specialties.forEach(s -> byName.put(normalize(s.getName()), s));

        // created_by solo si el admin tiene perfil (la columna es nullable);
        // evita romper la importación por una FK si falta el perfil.
        UUID creator = userProfileRepository.existsById(adminId) ? adminId : null;

        List<QuestionResponse> saved = new ArrayList<>();
        for (AiAdapter.ExtractedQuestion eq : questions) {
            if (eq.stem() == null || eq.stem().isBlank()
                    || eq.options() == null || eq.options().size() < 2) {
                continue;
            }
            List<String> opts = eq.options();
            Specialty specialty = matchSpecialty(eq.specialty(), byName, specialties);

            Question.Difficulty difficulty;
            try {
                difficulty = Question.Difficulty.valueOf(
                        (eq.difficulty() == null ? "MEDIA" : eq.difficulty().trim().toUpperCase()));
            } catch (Exception ex) {
                difficulty = Question.Difficulty.MEDIA;
            }

            String correct = (eq.correctOption() == null ? "A" : eq.correctOption().trim().toUpperCase());
            if (!correct.matches("[A-E]") || (correct.charAt(0) - 'A') >= opts.size()) {
                correct = "A";
            }

            Question q = Question.builder()
                    .specialty(specialty)
                    .stem(eq.stem().trim())
                    .optionA(opt(opts, 0))
                    .optionB(opt(opts, 1))
                    .optionC(opt(opts, 2))
                    .optionD(opt(opts, 3))
                    .optionE(opt(opts, 4))
                    .correctOption(correct)
                    .explanation(eq.explanation())
                    .difficulty(difficulty)
                    .source("Importado con IA")
                    .academy(academy)
                    .subsection(eq.subsection())
                    .createdBy(creator)
                    .build();

            saved.add(toQuestionResponse(questionRepository.save(q)));
        }

        log.info("Importación de examen: {} preguntas guardadas de {} detectadas", saved.size(), questions.size());
        return new ExamImportResponse(saved.size(), saved);
    }

    /**
     * Trocea el texto por preguntas usando varios formatos de numeración
     * ("Pregunta N", "N.", "N)", "PNNN"). Si no reconoce marcadores, trocea por
     * tamaño con SOLAPE, para que una pregunta partida entre trozos aparezca
     * completa en al menos uno (el deduplicado quita las repetidas).
     */
    private static List<String> splitIntoBatches(String text, int perBatch) {
        String marker = "(?m)(?=^\\s{0,4}(?:Pregunta\\s+\\d+\\b|\\d{1,3}\\s*[.)]\\s|P\\d{2,4}\\b))";
        List<String> blocks = Arrays.stream(text.split(marker))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
        List<String> batches = new ArrayList<>();
        if (blocks.size() >= 3) {
            for (int i = 0; i < blocks.size(); i += perBatch) {
                batches.add(String.join("\n\n", blocks.subList(i, Math.min(i + perBatch, blocks.size()))));
            }
        } else {
            int window = 7000, step = 5500; // ~1500 de solape
            for (int i = 0; i < text.length(); i += step) {
                batches.add(text.substring(i, Math.min(i + window, text.length())));
                if (i + window >= text.length()) break;
            }
        }
        return batches;
    }

    private static String opt(List<String> options, int i) {
        return i < options.size() ? options.get(i) : null;
    }

    private Specialty matchSpecialty(String name, Map<String, Specialty> byName, List<Specialty> all) {
        if (name != null && !name.isBlank()) {
            String n = normalize(name);
            Specialty exact = byName.get(n);
            if (exact != null) return exact;
            for (Specialty s : all) {
                String sn = normalize(s.getName());
                if (sn.contains(n) || n.contains(sn)) return s;
            }
        }
        return all.get(0); // fallback si la IA devolvió algo fuera de la lista
    }

    /** Clave normalizada del enunciado para detectar duplicados (sin tildes ni símbolos). */
    private static String stemKey(String stem) {
        String k = normalize(stem).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        return k.length() > 100 ? k.substring(0, 100) : k;
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private QuestionResponse toQuestionResponse(Question q) {
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
