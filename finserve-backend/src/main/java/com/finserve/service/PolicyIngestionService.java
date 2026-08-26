package com.finserve.service;

import com.finserve.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);

    private final SimpleVectorStoreService vectorStore;
    private final OpenAiEmbeddingClientService embeddingClient;

    public PolicyIngestionService(SimpleVectorStoreService vectorStore, OpenAiEmbeddingClientService embeddingClient) {
        this.vectorStore = vectorStore;
        this.embeddingClient = embeddingClient;
    }

    /**
     * Triggers on application startup. Reads all markdown files in classpath:policies/,
     * chunks them by header, embeds them, and loads them into the in-memory vector store.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ingestPoliciesOnStartup() {
        log.info("Starting policy document ingestion...");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:policies/*.md");

            if (resources.length == 0) {
                log.warn("No policy documents found in classpath:policies/*.md");
                return;
            }

            int totalChunks = 0;
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String content = readResource(resource);
                List<DocumentChunk> chunks = parseMarkdownIntoChunks(filename, content);
                
                for (DocumentChunk chunk : chunks) {
                    vectorStore.addChunk(chunk);
                    totalChunks++;
                }
                log.info("Ingested {} ({} chunks)", filename, chunks.size());
            }

            log.info("Policy ingestion complete. Loaded {} total chunks into vector store.", totalChunks);

        } catch (Exception e) {
            log.error("Failed to ingest policy documents: {}", e.getMessage(), e);
        }
    }

    private String readResource(Resource resource) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Splits a markdown document into chunks based on "## " headers.
     * The "# " (H1) header is assumed to be the document title.
     */
    private List<DocumentChunk> parseMarkdownIntoChunks(String filename, String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        String currentDocTitle = filename;
        String currentSection = "General";
        StringBuilder currentChunkContent = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("# ")) {
                currentDocTitle = line.substring(2).trim();
                // Optionally start a new chunk
                if (currentChunkContent.length() > 0) {
                    chunks.add(createChunk(currentDocTitle, currentSection, currentChunkContent.toString()));
                    currentChunkContent = new StringBuilder();
                }
            } else if (line.startsWith("## ")) {
                if (currentChunkContent.length() > 0) {
                    chunks.add(createChunk(currentDocTitle, currentSection, currentChunkContent.toString()));
                    currentChunkContent = new StringBuilder();
                }
                currentSection = line.substring(3).trim();
                currentChunkContent.append(line).append("\n");
            } else {
                currentChunkContent.append(line).append("\n");
            }
        }

        if (currentChunkContent.length() > 0 && !currentChunkContent.toString().trim().isEmpty()) {
            chunks.add(createChunk(currentDocTitle, currentSection, currentChunkContent.toString()));
        }

        return chunks;
    }

    private DocumentChunk createChunk(String docName, String section, String text) {
        String cleanText = text.trim();
        // Construct the embedding context
        String contextForEmbedding = "Document: " + docName + "\nSection: " + section + "\n" + cleanText;
        float[] vector = embeddingClient.getEmbedding(contextForEmbedding);
        return new DocumentChunk(docName, section, cleanText, vector);
    }
}
