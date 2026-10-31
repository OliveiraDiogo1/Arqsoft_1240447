package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;
import pt.psoft.g1.psoftg1.exceptions.GlobalExceptionHandler;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class GoogleBooksBibliographicSourceTest {

    @Test
    void mapsGoogleVolume() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new GoogleBooksBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withSuccess("""
                        {"items":[{"volumeInfo":{
                          "title":"O Príncipe",
                          "authors":["Nicolau Maquiavel"],
                          "description":"Descrição",
                          "publisher":"Editora",
                          "publishedDate":"1532",
                          "pageCount":120,
                          "categories":["Filosofia"],
                          "language":"pt",
                          "imageLinks":{"thumbnail":"https://img.example/cover.jpg"}
                        }}]}
                        """, MediaType.APPLICATION_JSON));

        var record = source.findByIsbn("9789720706386").orElseThrow();

        assertEquals("9789720706386", record.isbn());
        assertEquals("O Príncipe", record.title());
        assertEquals(List.of("Nicolau Maquiavel"), record.authors());
        assertEquals("Descrição", record.description());
        assertEquals("Editora", record.publisher());
        assertEquals("1532", record.publishedDate());
        assertEquals(120, record.pageCount());
        assertEquals(List.of("Filosofia"), record.categories());
        assertEquals("https://img.example/cover.jpg", record.thumbnailUrl());
        assertEquals("pt", record.language());
        assertEquals(List.of("google-books"), record.sources());
        server.verify();
    }

    @Test
    void emptyGoogleResultReturnsEmpty() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new GoogleBooksBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(source.findByIsbn("9789720706386").isEmpty());
        server.verify();
    }

    @Test
    void googleHttpErrorThrowsSourceException() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new GoogleBooksBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(BibliographicSourceException.class, () -> source.findByIsbn("9789720706386"));
        server.verify();
    }

    @Test
    void googleFailureDoesNotExposeApiKeyInExceptionOrLog(CapturedOutput output) {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var source = new GoogleBooksBibliographicSource(restTemplate, properties());
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(request -> {
                    throw new ResourceAccessException("I/O error on GET request for \"" + request.getURI() + "\"");
                });

        var exception = assertThrows(BibliographicSourceException.class,
                () -> source.findByIsbn("9789720706386"));
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/books/9789720706386/bibliographic");
        new GlobalExceptionHandler().handleBibliographicSourceException(request, exception);

        assertFalse(exception.getMessage().contains("test-key"));
        assertFalse(output.getAll().contains("test-key"));
        server.verify();
    }

    private static BibliographicProperties properties() {
        return new BibliographicProperties(
                List.of("google-books"),
                new BibliographicProperties.GoogleBooks("https://www.googleapis.com/books/v1", "test-key"),
                new BibliographicProperties.OpenLibrary("https://openlibrary.org"),
                new BibliographicProperties.Http(2000, 5000));
    }
}
