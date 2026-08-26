package com.finserve.service;

public interface OpenAiClientService {
    String chat(String systemPrompt, String userPrompt);
}
