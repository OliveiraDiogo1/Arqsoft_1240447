package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenLibraryBibliographicSourceTest {

    @Test
    void mapsOpenLibraryBook() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new OpenLibraryBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://openlibrary.org/api/books")))
                .andRespond(withSuccess("""
                        {"ISBN:9789720706386":{
                          "title":"O Príncipe",
                          "authors":[{"name":"Nicolau Maquiavel"}],
                          "publishers":[{"name":"Editora"}],
                          "publish_date":"1532",
                          "number_of_pages":120,
                          "subjects":[{"name":"Filosofia"}],
                          "cover":{"medium":"https://img.example/medium.jpg","small":"https://img.example/small.jpg"},
                          "description":"Descrição",
                          "language":"por"
                        }}
                        """, MediaType.APPLICATION_JSON));

        var record = source.findByIsbn("9789720706386").orElseThrow();

        assertEquals("open-library", source.name());
        assertEquals("9789720706386", record.isbn());
        assertEquals("O Príncipe", record.title());
        assertEquals(List.of("Nicolau Maquiavel"), record.authors());
        assertEquals("Descrição", record.description());
        assertEquals("Editora", record.publisher());
        assertEquals("1532", record.publishedDate());
        assertEquals(120, record.pageCount());
        assertEquals(List.of("Filosofia"), record.categories());
        assertEquals("https://img.example/medium.jpg", record.thumbnailUrl());
        assertEquals("por", record.language());
        assertEquals(List.of("open-library"), record.sources());
        server.verify();
    }

    @Test
    void usesNotesAndSmallCoverWhenPrimaryValuesAreMissing() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new OpenLibraryBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://openlibrary.org/api/books")))
                .andRespond(withSuccess("""
                        {"ISBN:9789720706386":{
                          "notes":"Notas do livro",
                          "cover":{"small":"https://img.example/small.jpg"}
                        }}
                        """, MediaType.APPLICATION_JSON));

        var record = source.findByIsbn("9789720706386").orElseThrow();

        assertEquals("Notas do livro", record.description());
        assertEquals("https://img.example/small.jpg", record.thumbnailUrl());
        assertNull(record.language());
        server.verify();
    }

    @Test
    void emptyOpenLibraryResultReturnsEmpty() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new OpenLibraryBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://openlibrary.org/api/books")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith("https://openlibrary.org/api/books")))
                .andRespond(withSuccess("{\"ISBN:9789720706386\":{}}", MediaType.APPLICATION_JSON));

        assertTrue(source.findByIsbn("9789720706386").isEmpty());
        assertTrue(source.findByIsbn("9789720706386").isEmpty());
        server.verify();
    }

    @Test
    void openLibraryHttpErrorThrowsSourceException() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new OpenLibraryBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://openlibrary.org/api/books")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(BibliographicSourceException.class, () -> source.findByIsbn("9789720706386"));
        server.verify();
    }

    private static BibliographicProperties properties() {
        return new BibliographicProperties(
                List.of("open-library"),
                new BibliographicProperties.GoogleBooks("https://www.googleapis.com/books/v1", "test-key"),
                new BibliographicProperties.OpenLibrary("https://openlibrary.org"),
                new BibliographicProperties.Http(2000, 5000));
    }
}
