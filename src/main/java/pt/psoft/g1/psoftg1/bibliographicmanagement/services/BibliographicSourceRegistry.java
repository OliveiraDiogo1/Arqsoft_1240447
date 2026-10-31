package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.BibliographicProperties;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicSource;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public final class BibliographicSourceRegistry {

    private final Map<String, BibliographicSource> sourcesByName = new LinkedHashMap<>();
    private final BibliographicProperties properties;

    public BibliographicSourceRegistry(List<BibliographicSource> sources, BibliographicProperties properties) {
        this.properties = properties;
        for (BibliographicSource source : sources) {
            String name = source.name();
            if (sourcesByName.putIfAbsent(name, source) != null) {
                throw new IllegalArgumentException("Duplicate bibliographic source: " + name);
            }
        }
        Set<String> configuredSources = new HashSet<>();
        for (String configured : properties.sources()) {
            if (!configuredSources.add(configured)) {
                throw new IllegalArgumentException("Duplicate configured bibliographic source: " + configured);
            }
            if (!sourcesByName.containsKey(configured)) {
                throw new IllegalArgumentException("Unknown bibliographic source: " + configured);
            }
        }
    }

    public List<BibliographicSource> selected() {
        return properties.sources().stream()
                .map(sourcesByName::get)
                .toList();
    }
}
