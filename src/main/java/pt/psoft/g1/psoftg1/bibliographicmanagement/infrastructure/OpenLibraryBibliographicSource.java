package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicSource;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenLibraryBibliographicSource implements BibliographicSource {

    private final RestTemplate restTemplate;
    private final BibliographicProperties.OpenLibrary properties;

    public OpenLibraryBibliographicSource(RestTemplate restTemplate, BibliographicProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties.openLibrary();
    }

    @Override
    public String name() {
        return "open-library";
    }

    @Override
    public Optional<BibliographicRecord> findByIsbn(String isbn) {
        var uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl())
                .path("/api/books")
                .queryParam("bibkeys", "ISBN:" + isbn)
                .queryParam("format", "json")
                .queryParam("jscmd", "data")
                .build()
                .toUri();

        try {
            var response = restTemplate.getForObject(uri, JsonNode.class);
            var book = response == null ? null : response.path("ISBN:" + isbn);
            if (book == null || book.isMissingNode() || book.isNull() || !book.isObject() || book.isEmpty()) {
                return Optional.empty();
            }

            var cover = book.path("cover");
            var record = new BibliographicRecord(
                    isbn,
                    text(book, "title"),
                    namedValues(book.path("authors")),
                    description(book),
                    firstNamedValue(book.path("publishers")),
                    text(book, "publish_date"),
                    integer(book, "number_of_pages"),
                    namedValues(book.path("subjects")),
                    firstText(text(cover, "medium"), text(cover, "small")),
                    text(book, "language"),
                    List.of(name()),
                    List.of());
            return Optional.of(record);
        } catch (RestClientException e) {
            throw new BibliographicSourceException("Open Library unavailable", e);
        }
    }

    private static String description(JsonNode book) {
        var description = textOrValue(book.path("description"));
        return description == null ? textOrValue(book.path("notes")) : description;
    }

    private static String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static String textOrValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        }
        return text(node, "value");
    }

    private static Integer integer(JsonNode node, String field) {
        var value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private static List<String> namedValues(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        var values = new ArrayList<String>();
        node.forEach(value -> {
            var name = text(value, "name");
            if (name != null) {
                values.add(name);
            }
        });
        return values;
    }

    private static String firstNamedValue(JsonNode node) {
        var values = namedValues(node);
        return values.isEmpty() ? null : values.get(0);
    }

    private static String firstText(String first, String fallback) {
        return first == null ? fallback : first;
    }
}
