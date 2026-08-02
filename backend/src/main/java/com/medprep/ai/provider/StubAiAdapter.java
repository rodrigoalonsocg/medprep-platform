package com.medprep.ai.provider;

import com.medprep.ai.adapter.AiAdapter;
import com.medprep.exception.MedPrepException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnMissingBean(AiAdapter.class)
public class StubAiAdapter implements AiAdapter {

    @Override
    public CompletableFuture<PatternAnalysisResult> analyzePattern(String clinicalCase) {
        return CompletableFuture.failedFuture(
                MedPrepException.badRequest("No hay proveedor de IA configurado. Configura AI_PROVIDER y la API key correspondiente.")
        );
    }

    @Override
    public CompletableFuture<List<FlashcardResult>> generateFlashcards(String questionStem, String correctAnswer, String explanation) {
        return CompletableFuture.failedFuture(
                MedPrepException.badRequest("No hay proveedor de IA configurado. Configura AI_PROVIDER y la API key correspondiente.")
        );
    }
}
