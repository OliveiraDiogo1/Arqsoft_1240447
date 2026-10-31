package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicSource;
import pt.psoft.g1.psoftg1.bookmanagement.model.Isbn;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
public class BibliographicServiceImpl implements BibliographicService {

    private final BibliographicSourceRegistry registry;

    public BibliographicServiceImpl(BibliographicSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Optional<BibliographicRecord> lookup(String isbn) {
        String normalized = normalize(isbn);
        new Isbn(normalized);

        List<BibliographicSource> selected = registry.selected();
        List<BibliographicRecord> records = new ArrayList<>();
        List<String> failedSources = new ArrayList<>();
        Set<String> contributingSources = new LinkedHashSet<>();
        Throwable firstFailure = null;

        for (BibliographicSource source : selected) {
            try {
                Optional<BibliographicRecord> found = source.findByIsbn(normalized);
                if (found.isPresent()) {
                    records.add(found.get());
                    contributingSources.add(source.name());
                }
            } catch (RuntimeException e) {
                failedSources.add(source.name());
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }

        if (records.isEmpty()) {
            if (failedSources.size() == selected.size()) {
                String message = "All bibliographic sources failed for ISBN " + normalized
                        + " (failed sources: " + String.join(", ", failedSources) + ")";
                throw firstFailure == null
                        ? new BibliographicSourceException(message)
                        : new BibliographicSourceException(message, firstFailure);
            }
            return Optional.empty();
        }

        return Optional.of(merge(normalized, records, contributingSources, failedSources));
    }

    private static String normalize(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replace("-", "").replace(" ", "");
    }

    private static BibliographicRecord merge(String isbn, List<BibliographicRecord> records,
                                             Set<String> sources, List<String> failedSources) {
        String title = firstNonBlank(records, BibliographicRecord::title);
        String description = firstNonBlank(records, BibliographicRecord::description);
        String publisher = firstNonBlank(records, BibliographicRecord::publisher);
        String publishedDate = firstNonBlank(records, BibliographicRecord::publishedDate);
        Integer pageCount = firstNonNull(records, BibliographicRecord::pageCount);
        String thumbnailUrl = firstNonBlank(records, BibliographicRecord::thumbnailUrl);
        String language = firstNonBlank(records, BibliographicRecord::language);

        Set<String> authors = new LinkedHashSet<>();
        Set<String> categories = new LinkedHashSet<>();
        for (BibliographicRecord record : records) {
            authors.addAll(record.authors());
            categories.addAll(record.categories());
        }

        return new BibliographicRecord(
                isbn,
                title,
                List.copyOf(authors),
                description,
                publisher,
                publishedDate,
                pageCount,
                List.copyOf(categories),
                thumbnailUrl,
                language,
                List.copyOf(sources),
                List.copyOf(failedSources));
    }

    private static String firstNonBlank(List<BibliographicRecord> records,
                                        Function<BibliographicRecord, String> getter) {
        return records.stream()
                .map(getter)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static Integer firstNonNull(List<BibliographicRecord> records,
                                        Function<BibliographicRecord, Integer> getter) {
        return records.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
