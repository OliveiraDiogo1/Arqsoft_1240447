package pt.psoft.g1.psoftg1.bibliographicmanagement.model;

import java.util.Optional;

public interface BibliographicSource {

    String name();

    Optional<BibliographicRecord> findByIsbn(String isbn);
}
