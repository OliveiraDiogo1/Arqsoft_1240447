package pt.psoft.g1.psoftg1.bibliographicmanagement.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.BibliographicService;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BibliographicController {

    private final BibliographicService service;

    @GetMapping("/{isbn}/bibliographic")
    public ResponseEntity<BibliographicRecordView> lookup(@PathVariable String isbn) {
        return service.lookup(isbn)
                .map(record -> ResponseEntity.ok(BibliographicRecordView.from(record)))
                .orElseThrow(() -> new NotFoundException("No bibliographic information found for ISBN " + isbn));
    }
}
