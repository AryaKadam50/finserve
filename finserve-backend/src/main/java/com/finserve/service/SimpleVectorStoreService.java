package com.finserve.service;

import com.finserve.model.DocumentChunk;
import java.util.List;

public interface SimpleVectorStoreService {
    void addChunk(DocumentChunk chunk);
    void addChunks(List<DocumentChunk> newChunks);
    void clear();
    int size();
    List<SimpleVectorStore.SearchResult> search(float[] queryVector, int topK);
}
