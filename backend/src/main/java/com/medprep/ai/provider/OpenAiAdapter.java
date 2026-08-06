package com.medprep.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.medprep.ai.adapter.AiAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiAdapter implements AiAdapter {

    private final WebClient webClient;

    @Value("${ai.openai.model}")
    private String model;

    public OpenAiAdapter(@Value("${ai.openai.api-key}") String apiKey,
                         @Value("${ai.openai.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public CompletableFuture<PatternAnalysisResult> analyzePattern(String clinicalCase) {
        String prompt = buildPatternPrompt(clinicalCase);
        return callChatCompletion(prompt).thenApply(this::parsePatternResult);
    }

    @Override
    public CompletableFuture<List<FlashcardResult>> generateFlashcards(String questionStem,
                                                                         String correctAnswer,
                                                                         String explanation) {
        String prompt = buildFlashcardPrompt(questionStem, correctAnswer, explanation);
        return callChatCompletion(prompt).thenApply(this::parseFlashcards);
    }

    @Override
    public CompletableFuture<List<ExtractedQuestion>> extractQuestions(String examText,
                                                                        List<String> specialtyNames) {
        String prompt = AiAdapter.buildExtractPrompt(examText, specialtyNames);
        return callChatCompletion(prompt, 8192).thenApply(AiAdapter::parseExtractedJson);
    }

    private CompletableFuture<String> callChatCompletion(String prompt) {
        return callChatCompletion(prompt, 1024);
    }

    private CompletableFuture<String> callChatCompletion(String prompt, int maxTokens) {
        var body = java.util.Map.of(
                "model", model,
                "messages", java.util.List.of(
                        java.util.Map.of("role", "system",
                                "content", "Eres un tutor experto en medicina para el internado médico peruano. Responde siempre en JSON válido sin texto adicional."),
                        java.util.Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", maxTokens,
                "response_format", java.util.Map.of("type", "json_object")
        );
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.at("/choices/0/message/content").asText())
                .toFuture();
    }

    private String buildPatternPrompt(String clinicalCase) {
        return """
                Eres un tutor experto en medicina para el internado médico peruano (ENAM/EsSalud).
                Analiza este caso clínico y responde en formato JSON con esta estructura exacta:
                {
                  "keywords": ["keyword1", "keyword2"],
                  "diagnosis": "diagnóstico principal",
                  "pearl": "perla médica o regla nemotécnica",
                  "distractors": ["distractor1", "distractor2"]
                }

                Caso clínico: %s
                """.formatted(clinicalCase);
    }

    private String buildFlashcardPrompt(String stem, String answer, String explanation) {
        return """
                Genera 2-3 flashcards médicas a partir de esta pregunta. Responde en JSON:
                {"flashcards": [{"front": "pregunta concisa", "back": "respuesta directa"}]}

                Enunciado: %s
                Respuesta correcta: %s
                Explicación: %s
                """.formatted(stem, answer, explanation);
    }

    private PatternAnalysisResult parsePatternResult(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            var listType = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return new PatternAnalysisResult(
                    mapper.convertValue(node.get("keywords"), listType),
                    node.path("diagnosis").asText(),
                    node.path("pearl").asText(),
                    mapper.convertValue(node.get("distractors"), listType)
            );
        } catch (Exception e) {
            log.error("Error parseando respuesta de IA: {}", e.getMessage());
            return new PatternAnalysisResult(List.of(), "", "", List.of());
        }
    }

    private List<FlashcardResult> parseFlashcards(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            return mapper.convertValue(node.get("flashcards"),
                    mapper.getTypeFactory().constructCollectionType(List.class, FlashcardResult.class));
        } catch (Exception e) {
            log.error("Error parseando flashcards de IA: {}", e.getMessage());
            return List.of();
        }
    }
}
