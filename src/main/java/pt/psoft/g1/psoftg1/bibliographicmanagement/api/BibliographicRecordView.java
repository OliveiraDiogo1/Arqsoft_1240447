package pt.psoft.g1.psoftg1.bibliographicmanagement.api;

import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;

import java.util.List;

public record BibliographicRecordView(
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

    public static BibliographicRecordView from(BibliographicRecord record) {
        return new BibliographicRecordView(
                record.isbn(),
                record.title(),
                record.authors(),
                record.description(),
                record.publisher(),
                record.publishedDate(),
                record.pageCount(),
                record.categories(),
                record.thumbnailUrl(),
                record.language(),
                record.sources(),
                record.failedSources());
    }
}
