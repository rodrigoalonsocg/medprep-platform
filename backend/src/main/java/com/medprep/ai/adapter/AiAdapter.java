package com.medprep.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AiAdapter {

    CompletableFuture<PatternAnalysisResult> analyzePattern(String clinicalCase);

    CompletableFuture<List<FlashcardResult>> generateFlashcards(String questionStem,
                                                                  String correctAnswer,
                                                                  String explanation);

    /**
     * Detecta y clasifica preguntas de opción múltiple a partir del texto de un
     * examen. Debe clasificar cada pregunta en UNA de las especialidades provistas
     * (por nombre exacto).
     */
    CompletableFuture<List<ExtractedQuestion>> extractQuestions(String examText,
                                                                 List<String> specialtyNames);

    record PatternAnalysisResult(
            List<String> keywords,
            String diagnosis,
            String pearl,
            List<String> distractors
    ) {}

    record FlashcardResult(
            String front,
            String back
    ) {}

    /** Pregunta detectada por la IA a partir de un examen. */
    record ExtractedQuestion(
            String stem,
            List<String> options,   // en orden A, B, C, D, E
            String correctOption,   // "A".."E"
            String explanation,
            String difficulty,      // BAJA | MEDIA | ALTA
            String specialty        // nombre exacto de una de las especialidades provistas
    ) {}

    /** Prompt compartido para extraer y clasificar preguntas de un examen. */
    static String buildExtractPrompt(String examText, List<String> specialtyNames) {
        String specialties = String.join(", ", specialtyNames);
        return """
                Eres un experto en medicina para el internado médico peruano (ENAM/EsSalud).
                A continuación tienes el TEXTO de un examen con preguntas de opción múltiple
                que YA incluyen la respuesta correcta.

                Tu tarea: detectar CADA pregunta y devolverla estructurada. Para cada una:
                - Extrae el enunciado (caso clínico) completo.
                - Extrae las opciones en orden (A, B, C, D, E). Puede haber de 2 a 5.
                - Identifica la opción correcta como letra ("A".."E").
                - Escribe una explicación breve del porqué (si el examen no la trae, genérala tú).
                - Estima la dificultad: "BAJA", "MEDIA" o "ALTA".
                - Clasifica la pregunta en EXACTAMENTE UNA de estas especialidades (usa el nombre EXACTO):
                  %s

                Responde SOLO con JSON válido, sin markdown, con esta estructura EXACTA:
                {"questions":[{"stem":"...","options":["...","..."],"correctOption":"A","explanation":"...","difficulty":"MEDIA","specialty":"..."}]}

                Si no encuentras preguntas, responde {"questions":[]}.

                TEXTO DEL EXAMEN:
                %s
                """.formatted(specialties, examText);
    }

    /** Parsea la respuesta JSON de la IA (tolerando fences ```json) a la lista de preguntas. */
    static List<ExtractedQuestion> parseExtractedJson(String json) {
        try {
            String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            var node = mapper.readTree(cleaned).get("questions");
            if (node == null) return List.of();
            var type = mapper.getTypeFactory().constructCollectionType(List.class, ExtractedQuestion.class);
            return mapper.convertValue(node, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}
