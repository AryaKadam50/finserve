package com.finserve.service;

import com.finserve.model.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A lightweight, in-memory vector store that holds policy documents
 * and performs cosine similarity search.
 */
@Service
public class SimpleVectorStore implements SimpleVectorStoreService {

    private final List<DocumentChunk> chunks = new ArrayList<>();

    @Override
    public void addChunk(DocumentChunk chunk) {
        if (chunk != null && chunk.getEmbedding() != null) {
            chunks.add(chunk);
        }
    }
    
    @Override
    public void addChunks(List<DocumentChunk> newChunks) {
        if (newChunks != null) {
            chunks.addAll(newChunks);
        }
    }

    @Override
    public void clear() {
        chunks.clear();
    }

    @Override
    public int size() {
        return chunks.size();
    }

    /**
     * Searches the vector store for the top K most similar chunks to the query vector.
     */
    @Override
    public List<SimpleVectorStore.SearchResult> search(float[] queryVector, int topK) {
        if (chunks.isEmpty() || queryVector == null) {
            return new ArrayList<>();
        }

        return chunks.stream()
                .map(chunk -> new SearchResult(chunk, cosineSimilarity(queryVector, chunk.getEmbedding())))
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * Computes the cosine similarity between two vectors.
     * Assumes vectors are of the same dimension.
     */
    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += Math.pow(v1[i], 2);
            normB += Math.pow(v2[i], 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class SearchResult {
        private final DocumentChunk chunk;
        private final double score;

        public SearchResult(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        public DocumentChunk getChunk() {
            return chunk;
        }

        public double getScore() {
            return score;
        }
    }
}
