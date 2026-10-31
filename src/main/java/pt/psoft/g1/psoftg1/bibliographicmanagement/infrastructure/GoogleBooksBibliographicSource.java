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
public class GoogleBooksBibliographicSource implements BibliographicSource {

    private final RestTemplate restTemplate;
    private final BibliographicProperties.GoogleBooks properties;

    public GoogleBooksBibliographicSource(RestTemplate restTemplate, BibliographicProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties.googleBooks();
    }

    @Override
    public String name() {
        return "google-books";
    }

    @Override
    public Optional<BibliographicRecord> findByIsbn(String isbn) {
        var uriBuilder = UriComponentsBuilder.fromHttpUrl(properties.baseUrl())
                .path("/volumes")
                .queryParam("q", "isbn:" + isbn);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            uriBuilder.queryParam("key", properties.apiKey());
        }

        try {
            var uri = uriBuilder.build().toUri();
            var response = restTemplate.getForObject(uri, JsonNode.class);
            var items = response == null ? null : response.path("items");
            if (items == null || !items.isArray() || items.isEmpty()) {
                return Optional.empty();
            }

            var volumeInfo = items.path(0).path("volumeInfo");
            if (volumeInfo.isMissingNode() || volumeInfo.isNull()) {
                return Optional.empty();
            }

            var imageLinks = volumeInfo.path("imageLinks");
            var record = new BibliographicRecord(
                    isbn,
                    text(volumeInfo, "title"),
                    texts(volumeInfo.path("authors")),
                    text(volumeInfo, "description"),
                    text(volumeInfo, "publisher"),
                    text(volumeInfo, "publishedDate"),
                    integer(volumeInfo, "pageCount"),
                    texts(volumeInfo.path("categories")),
                    text(imageLinks, "thumbnail"),
                    text(volumeInfo, "language"),
                    List.of(name()),
                    List.of());
            return Optional.of(record);
        } catch (RestClientException e) {
            throw new BibliographicSourceException("Google Books unavailable", e);
        }
    }

    private static String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static Integer integer(JsonNode node, String field) {
        var value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private static List<String> texts(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        var values = new ArrayList<String>();
        node.forEach(value -> {
            if (value.isTextual()) {
                values.add(value.asText());
            }
        });
        return values;
    }
}
