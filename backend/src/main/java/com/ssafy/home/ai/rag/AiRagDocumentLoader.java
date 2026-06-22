package com.ssafy.home.ai.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AiRagDocumentLoader {

    private static final int MAX_CHUNK_LENGTH = 1_600;

    private final VectorStore vectorStore;

    @Value("${app.ai.rag.docs-path:../docs/ai}")
    private String docsPath;

    @EventListener(ApplicationReadyEvent.class)
    public void loadDocuments() {
        Path directory = resolveDocsDirectory();
        if (directory == null) {
            return;
        }

        try {
            List<Document> documents = Files.list(directory)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase("README.md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .flatMap(path -> toDocuments(path).stream())
                    .toList();

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }
        } catch (IOException ignored) {
            // RAG is optional at boot. The service will answer safely if documents are unavailable.
        }
    }

    private Path resolveDocsDirectory() {
        List<Path> candidates = List.of(
                Path.of(docsPath),
                Path.of("docs", "ai"),
                Path.of("..", "docs", "ai")
        );
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(null);
    }

    private List<Document> toDocuments(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String title = extractTitle(content, path);
            List<Section> sections = splitSections(content, title);
            List<Document> documents = new ArrayList<>();

            int index = 1;
            for (Section section : sections) {
                for (String chunk : splitLongSection(section.text())) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("docId", path.getFileName().toString() + "#" + index);
                    metadata.put("title", title + " - " + section.heading());
                    metadata.put("source", path.getFileName().toString());
                    metadata.put("section", section.heading());
                    metadata.put("chunkIndex", index);
                    documents.add(new Document(chunk, metadata));
                    index++;
                }
            }
            return documents;
        } catch (IOException e) {
            return List.of();
        }
    }

    private String extractTitle(String content, Path path) {
        return content.lines()
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .findFirst()
                .orElse(path.getFileName().toString());
    }

    private List<Section> splitSections(String content, String fallbackTitle) {
        List<Section> sections = new ArrayList<>();
        String currentHeading = fallbackTitle;
        StringBuilder current = new StringBuilder();

        for (String line : content.split("\\R")) {
            if (line.startsWith("## ")) {
                addSection(sections, currentHeading, current);
                currentHeading = line.substring(3).trim();
                current = new StringBuilder();
                continue;
            }
            if (!line.startsWith("# ")) {
                current.append(line).append(System.lineSeparator());
            }
        }
        addSection(sections, currentHeading, current);
        return sections;
    }

    private void addSection(List<Section> sections, String heading, StringBuilder content) {
        String text = content.toString().trim();
        if (!text.isBlank()) {
            sections.add(new Section(heading, text));
        }
    }

    private List<String> splitLongSection(String text) {
        if (text.length() <= MAX_CHUNK_LENGTH) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\R\\s*\\R");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() + 2 > MAX_CHUNK_LENGTH && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(paragraph).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private record Section(String heading, String text) {
    }
}
