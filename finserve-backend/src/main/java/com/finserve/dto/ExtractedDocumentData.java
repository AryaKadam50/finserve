package com.finserve.dto;

import java.util.HashMap;
import java.util.Map;

public class ExtractedDocumentData {
    private Map<String, String> extractedFields = new HashMap<>();

    public ExtractedDocumentData() {}

    public Map<String, String> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(Map<String, String> extractedFields) {
        this.extractedFields = extractedFields;
    }

    public void addField(String key, String value) {
        this.extractedFields.put(key, value);
    }
}
