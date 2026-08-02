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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek")
public class DeepSeekAdapter implements AiAdapter {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.deepseek.model}")
    private String model;

    public DeepSeekAdapter(@Value("${ai.deepseek.api-key}") String apiKey,
                           @Value("${ai.deepseek.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
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
        String body = """
                {"model":"%s","messages":[{"role":"system","content":"Eres un tutor experto en medicina para el internado médico peruano. Responde siempre en JSON válido sin texto adicional."},{"role":"user","content":"%s"}],"temperature":0.3}
                """.formatted(model, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.at("/choices/0/message/content").asText())
                .toFuture();
    }

    private String buildPatternPrompt(String clinicalCase) {
        return "Analiza este caso clínico e identifica los patrones diagnósticos. Responde SOLO con este JSON: {\"keywords\":[\"dato1\",\"dato2\"],\"diagnosis\":\"diagnóstico\",\"pearl\":\"perla médica\",\"distractors\":[\"distractor1\"]}. Caso: " + clinicalCase;
    }

    private String buildFlashcardPrompt(String stem, String answer, String explanation) {
        return "Crea 2-3 flashcards médicas concisas. Responde SOLO con este JSON: {\"flashcards\":[{\"front\":\"pregunta\",\"back\":\"respuesta\"}]}. Pregunta: " + stem + " Respuesta correcta: " + answer + " Explicación: " + explanation;
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
            log.error("Error parseando patrón de IA: {}", e.getMessage());
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
            log.error("Error parseando flashcards de IA: {}", e.getMessage());
            return List.of();
        }
    }
}
