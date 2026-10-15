import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import OpenAI from 'openai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export class RagPolicyService {
  static chunks = [];
  static isInitialized = false;

  /**
   * Reads all markdown policies in policies/ and chunks them by H2 headers.
   */
  static async initialize() {
    if (this.isInitialized) return;

    const policiesDir = path.join(__dirname, '../policies');
    if (!fs.existsSync(policiesDir)) {
      console.warn('[RAG] Policies directory not found at:', policiesDir);
      return;
    }

    const files = fs.readdirSync(policiesDir).filter((file) => file.endsWith('.md'));
    const loadedChunks = [];

    for (const file of files) {
      const filePath = path.join(policiesDir, file);
      const content = fs.readFileSync(filePath, 'utf-8');
      const lines = content.split('\n');

      let currentDocTitle = file;
      let currentSection = 'General';
      let currentChunkText = [];

      for (const line of lines) {
        if (line.startsWith('# ')) {
          currentDocTitle = line.substring(2).trim();
          if (currentChunkText.length > 0) {
            loadedChunks.push({
              docName: currentDocTitle,
              section: currentSection,
              text: currentChunkText.join('\n').trim(),
            });
            currentChunkText = [];
          }
        } else if (line.startsWith('## ')) {
          if (currentChunkText.length > 0) {
            loadedChunks.push({
              docName: currentDocTitle,
              section: currentSection,
              text: currentChunkText.join('\n').trim(),
            });
            currentChunkText = [];
          }
          currentSection = line.substring(3).trim();
          currentChunkText.push(line);
        } else {
          currentChunkText.push(line);
        }
      }

      if (currentChunkText.length > 0 && currentChunkText.join('\n').trim().length > 0) {
        loadedChunks.push({
          docName: currentDocTitle,
          section: currentSection,
          text: currentChunkText.join('\n').trim(),
        });
      }
    }

    // Embed all chunks
    for (const chunk of loadedChunks) {
      const context = `Document: ${chunk.docName}\nSection: ${chunk.section}\n${chunk.text}`;
      chunk.embedding = await this.getEmbedding(context);
    }

    this.chunks = loadedChunks;
    this.isInitialized = true;
    console.log(`[RAG] Policy ingestion complete. Loaded ${this.chunks.length} total chunks into vector store.`);
  }

  /**
   * Generates embedding via OpenAI or deterministic mock embedding
   */
  static async getEmbedding(text) {
    const apiKey = process.env.OPENAI_API_KEY;

    if (apiKey && apiKey.trim().length > 0) {
      try {
        const openai = new OpenAI({ apiKey });
        const res = await openai.embeddings.create({
          model: 'text-embedding-3-small',
          input: text,
        });
        return res.data[0].embedding;
      } catch (err) {
        console.warn('[RAG] OpenAI Embedding failed, falling back to deterministic vector:', err.message);
      }
    }

    // Deterministic 1536-dimensional mock embedding based on string hash
    return this.generateMockEmbedding(text);
  }

  static generateMockEmbedding(text) {
    const dim = 1536;
    const vector = new Array(dim);
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
      hash = (hash << 5) - hash + text.charCodeAt(i);
      hash |= 0;
    }

    let magnitude = 0;
    const lower = text.toLowerCase();
    for (let i = 0; i < dim; i++) {
      const mixed = (hash ^ (i * 31)) * 17;
      vector[i] = (mixed % 1000) / 1000.0;

      if (lower.includes('dti') && i % 10 === 0) vector[i] += 0.5;
      if (lower.includes('income') && i % 11 === 0) vector[i] += 0.5;
      if (lower.includes('credit') && i % 12 === 0) vector[i] += 0.5;
      if (lower.includes('review') && i % 13 === 0) vector[i] += 0.5;

      magnitude += vector[i] * vector[i];
    }

    magnitude = Math.sqrt(magnitude);
    if (magnitude > 0) {
      for (let i = 0; i < dim; i++) {
        vector[i] /= magnitude;
      }
    }
    return vector;
  }

  /**
   * Cosine similarity between two float vectors
   */
  static cosineSimilarity(v1, v2) {
    if (!v1 || !v2 || v1.length !== v2.length) return 0;
    let dot = 0;
    let normA = 0;
    let normB = 0;
    for (let i = 0; i < v1.length; i++) {
      dot += v1[i] * v2[i];
      normA += v1[i] * v1[i];
      normB += v2[i] * v2[i];
    }
    if (normA === 0 || normB === 0) return 0;
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /**
   * Search for top-K matching policy chunks
   */
  static async search(queryText, topK = 3) {
    await this.initialize();
    if (this.chunks.length === 0) return [];

    const queryVector = await this.getEmbedding(queryText);

    const scored = this.chunks.map((chunk) => ({
      chunk,
      score: this.cosineSimilarity(queryVector, chunk.embedding),
    }));

    scored.sort((a, b) => b.score - a.score);
    return scored.slice(0, topK).map((s) => s.chunk);
  }
}
