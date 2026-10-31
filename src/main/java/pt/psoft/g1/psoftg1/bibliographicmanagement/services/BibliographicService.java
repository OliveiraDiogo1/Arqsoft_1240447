package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;

import java.util.Optional;

public interface BibliographicService {

    Optional<BibliographicRecord> lookup(String isbn);
}
