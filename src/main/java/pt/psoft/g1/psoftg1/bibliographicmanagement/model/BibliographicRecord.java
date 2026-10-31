package pt.psoft.g1.psoftg1.bibliographicmanagement.model;

import java.util.List;

public record BibliographicRecord(
        String isbn,
        String title,
        List<String> authors,
        String description,
        String publisher,
        String publishedDate,
        Integer pageCount,
        List<String> categories,
        String thumbnailUrl,
        String language,
        List<String> sources,
        List<String> failedSources) {

    public BibliographicRecord {
        authors = authors == null ? List.of() : List.copyOf(authors);
        categories = categories == null ? List.of() : List.copyOf(categories);
        sources = sources == null ? List.of() : List.copyOf(sources);
        failedSources = failedSources == null ? List.of() : List.copyOf(failedSources);
    }
}
