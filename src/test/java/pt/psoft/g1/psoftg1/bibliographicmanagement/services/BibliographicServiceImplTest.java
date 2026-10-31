package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicSource;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BibliographicServiceImplTest {

    @Test
    void normalizesIsbnWithHyphens() {
        var source = source("google-books", record("9789720706386", "Título A", List.of("Autor A")));
        var service = new BibliographicServiceImpl(registryOf(source));
        assertTrue(service.lookup("978-972-0-70638-6").isPresent());
    }

    @Test
    void invalidIsbnFailsBeforeCallingSources() {
        var source = mock(BibliographicSource.class);
        when(source.name()).thenReturn("google-books");
        var service = new BibliographicServiceImpl(registryOf(source));
        assertThrows(IllegalArgumentException.class, () -> service.lookup("123"));
        verify(source, never()).findByIsbn(anyString());
    }

    @Test
    void mergesAvailableSourcesAndReportsFailedSource() {
        var first = source("google-books", record("9789720706386", "Título A", List.of("Autor A")));
        var broken = sourceThatThrows("open-library");
        var service = new BibliographicServiceImpl(registryOf(first, broken));

        var result = service.lookup("978-972-0-70638-6");

        assertTrue(result.isPresent());
        assertEquals("Título A", result.get().title());
        assertEquals(List.of("google-books"), result.get().sources());
        assertEquals(List.of("open-library"), result.get().failedSources());
    }

    @Test
    void allSourcesFailingThrowsBibliographicSourceException() {
        var service = new BibliographicServiceImpl(registryOf(
                sourceThatThrows("google-books"), sourceThatThrows("open-library")));
        assertThrows(BibliographicSourceException.class, () -> service.lookup("9789720706386"));
    }

    @Test
    void noSourceDataReturnsEmpty() {
        var service = new BibliographicServiceImpl(registryOf(source("google-books", null)));
        assertTrue(service.lookup("9789720706386").isEmpty());
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

    private static BibliographicSource sourceThatThrows(String name) {
        return new BibliographicSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<BibliographicRecord> findByIsbn(String isbn) {
                throw new RuntimeException("External failure for " + name);
            }
        };
    }

    private static BibliographicRecord record(String isbn, String title, List<String> authors) {
        return new BibliographicRecord(isbn, title, authors, null, null, null, null, null, null, null, List.of(), List.of());
    }

    private static BibliographicSourceRegistry registryOf(BibliographicSource... sources) {
        return new BibliographicSourceRegistry(
                Arrays.asList(sources),
                properties(Arrays.stream(sources).map(BibliographicSource::name).toList()));
    }

    private static BibliographicProperties properties(List<String> sources) {
        return new BibliographicProperties(
                sources,
                new BibliographicProperties.GoogleBooks("https://www.googleapis.com/books/v1", null),
                new BibliographicProperties.OpenLibrary("https://openlibrary.org"),
                new BibliographicProperties.Http(2000, 5000));
    }
}
