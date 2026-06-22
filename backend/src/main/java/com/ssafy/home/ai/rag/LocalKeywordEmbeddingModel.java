package com.ssafy.home.ai.rag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

public class LocalKeywordEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 384;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(embed(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : tokenize(text)) {
            addToken(vector, token, 1.0f);
            addNgrams(vector, token);
        }
        normalize(vector);
        return vector;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private List<String> tokenize(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private void addNgrams(float[] vector, String token) {
        if (token.length() < 2) {
            return;
        }
        int max = Math.min(4, token.length());
        for (int n = 2; n <= max; n++) {
            for (int i = 0; i <= token.length() - n; i++) {
                addToken(vector, token.substring(i, i + n), 0.35f);
            }
        }
    }

    private void addToken(float[] vector, String token, float weight) {
        int hash = token.hashCode();
        int index = Math.floorMod(hash, vector.length);
        float sign = (hash & 1) == 0 ? 1.0f : -1.0f;
        vector[index] += sign * weight;
    }

    private void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
