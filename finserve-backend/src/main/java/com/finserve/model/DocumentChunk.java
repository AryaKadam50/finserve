package com.finserve.model;

import java.util.Arrays;

/**
 * Represents a single chunk of a policy document and its vector embedding.
 */
public class DocumentChunk {

    private String documentName;
    private String sectionName;
    private String content;
    private float[] embedding;

    public DocumentChunk(String documentName, String sectionName, String content, float[] embedding) {
        this.documentName = documentName;
        this.sectionName = sectionName;
        this.content = content;
        this.embedding = embedding;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getContent() {
        return content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
                "documentName='" + documentName + '\'' +
                ", sectionName='" + sectionName + '\'' +
                ", content.length=" + (content != null ? content.length() : 0) +
                '}';
    }
}
