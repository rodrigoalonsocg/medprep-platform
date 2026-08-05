package com.medprep.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medprep.ai.adapter.AiAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAdapter implements AiAdapter {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model;
    private final String apiKey;

    public GeminiAdapter(@Value("${ai.gemini.api-key}") String apiKey,
                         @Value("${ai.gemini.base-url}") String baseUrl,
                         @Value("${ai.gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public CompletableFuture<PatternAnalysisResult> analyzePattern(String clinicalCase) {
        return call(buildPatternPrompt(clinicalCase)).thenApply(this::parsePattern);
    }

    @Override
    public CompletableFuture<List<FlashcardResult>> generateFlashcards(String stem, String answer, String explanation) {
        return call(buildFlashcardPrompt(stem, answer, explanation)).thenApply(this::parseFlashcards);
    }

    private CompletableFuture<String> call(String prompt) {
        var body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of("temperature", 0.3)
        );
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.at("/candidates/0/content/parts/0/text").asText())
                .toFuture();
    }

    private String buildPatternPrompt(String clinicalCase) {
        return """
                Eres un tutor experto en medicina para el internado médico peruano (ENAM/EsSalud).
                Analiza este caso clínico y responde SOLO con este JSON (sin markdown):
                {"keywords":["keyword1","keyword2"],"diagnosis":"diagnóstico principal","pearl":"perla médica","distractors":["distractor1","distractor2"]}

                Caso clínico: %s
                """.formatted(clinicalCase);
    }

    private String buildFlashcardPrompt(String stem, String answer, String explanation) {
        return """
                Genera 2-3 flashcards médicas concisas. Responde SOLO con este JSON (sin markdown):
                {"flashcards":[{"front":"pregunta concisa","back":"respuesta directa"}]}

                Enunciado: %s
                Respuesta correcta: %s
                Explicación: %s
                """.formatted(stem, answer, explanation);
    }

    private PatternAnalysisResult parsePattern(String json) {
        try {
            String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            var node = mapper.readTree(cleaned);
            var listType = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return new PatternAnalysisResult(
                    mapper.convertValue(node.get("keywords"), listType),
                    node.path("diagnosis").asText(),
                    node.path("pearl").asText(),
                    mapper.convertValue(node.get("distractors"), listType)
            );
        } catch (Exception e) {
            log.error("Error parseando patrón de IA (Gemini): {}", e.getMessage());
            return new PatternAnalysisResult(List.of(), "", "", List.of());
        }
    }

    private List<FlashcardResult> parseFlashcards(String json) {
        try {
            String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            var node = mapper.readTree(cleaned);
            var type = mapper.getTypeFactory().constructCollectionType(List.class, FlashcardResult.class);
            return mapper.convertValue(node.get("flashcards"), type);
        } catch (Exception e) {
            log.error("Error parseando flashcards de IA (Gemini): {}", e.getMessage());
            return List.of();
        }
    }
}
