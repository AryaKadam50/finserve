package com.finserve.service;

public interface OpenAiEmbeddingClientService {
    float[] getEmbedding(String text);
}
