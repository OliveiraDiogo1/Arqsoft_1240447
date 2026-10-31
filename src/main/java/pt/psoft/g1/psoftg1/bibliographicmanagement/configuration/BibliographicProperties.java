package pt.psoft.g1.psoftg1.bibliographicmanagement.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "bibliographic")
public record BibliographicProperties(
        @NotEmpty List<String> sources,
        @Valid @NotNull GoogleBooks googleBooks,
        @Valid @NotNull OpenLibrary openLibrary,
        @Valid @NotNull Http http) {

    public record GoogleBooks(@NotBlank String baseUrl, String apiKey) {}

    public record OpenLibrary(@NotBlank String baseUrl) {}

    public record Http(@Min(1) int connectTimeoutMs, @Min(1) int readTimeoutMs) {}
}
