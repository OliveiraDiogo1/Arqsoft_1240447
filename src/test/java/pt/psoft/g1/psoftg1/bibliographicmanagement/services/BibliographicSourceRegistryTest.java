package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BibliographicSourceRegistryTest {

    @Test
    void unknownSourceFailsAtStartup() {
        var props = properties(List.of("google-books", "inexistente"));
        assertThrows(IllegalArgumentException.class,
                () -> new BibliographicSourceRegistry(List.of(source("google-books", null)), props));
    }

    @Test
    void preservesConfiguredOrder() {
        var registry = new BibliographicSourceRegistry(
                List.of(source("open-library", null), source("google-books", null)),
                properties(List.of("google-books", "open-library")));
        assertEquals(List.of("google-books", "open-library"),
                registry.selected().stream().map(BibliographicSource::name).toList());
    }

    @Test
    void duplicateConfiguredSourceFailsAtStartup() {
        assertThrows(IllegalArgumentException.class,
                () -> new BibliographicSourceRegistry(
                        List.of(source("google-books", null)),
                        properties(List.of("google-books", "google-books"))));
    }

    @Test
    void duplicateSourceNameFailsAtStartup() {
        assertThrows(IllegalArgumentException.class,
                () -> new BibliographicSourceRegistry(
                        List.of(source("google-books", null), source("google-books", null)),
                        properties(List.of("google-books"))));
    }

    private static BibliographicSource source(String name, BibliographicRecord record) {
        return new BibliographicSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<BibliographicRecord> findByIsbn(String isbn) {
                return Optional.ofNullable(record);
            }
        };
    }

    private static BibliographicProperties properties(List<String> sources) {
        return new BibliographicProperties(
                sources,
                new BibliographicProperties.GoogleBooks("https://www.googleapis.com/books/v1", null),
                new BibliographicProperties.OpenLibrary("https://openlibrary.org"),
                new BibliographicProperties.Http(2000, 5000));
    }
}
