package com.medprep.ai.adapter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AiAdapter {

    CompletableFuture<PatternAnalysisResult> analyzePattern(String clinicalCase);

    CompletableFuture<List<FlashcardResult>> generateFlashcards(String questionStem,
                                                                  String correctAnswer,
                                                                  String explanation);

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
}
