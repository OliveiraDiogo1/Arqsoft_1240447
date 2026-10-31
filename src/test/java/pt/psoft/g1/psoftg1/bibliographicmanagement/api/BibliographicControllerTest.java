package pt.psoft.g1.psoftg1.bibliographicmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.BibliographicRecord;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.BibliographicService;
import pt.psoft.g1.psoftg1.exceptions.BibliographicSourceException;
import pt.psoft.g1.psoftg1.exceptions.GlobalExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BibliographicControllerTest {

    private final BibliographicService service = mock(BibliographicService.class);
    private final BibliographicRecord record = new BibliographicRecord(
            "9789720706386", "O Príncipe", List.of("Maquiavel"), "Descrição", "Editora",
            "1532", 128, List.of("Política"), "https://example.com/cover.jpg", "pt",
            List.of("google-books"), List.of());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BibliographicController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsRecord() throws Exception {
        when(service.lookup("9789720706386")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/books/9789720706386/bibliographic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("O Príncipe"))
                .andExpect(jsonPath("$.sources[0]").value("google-books"));
    }

    @Test
    void returnsNotFoundWhenNoSourceHasData() throws Exception {
        when(service.lookup("9789720706386")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/9789720706386/bibliographic"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadGatewayWhenAllSourcesFail() throws Exception {
        when(service.lookup("9789720706386"))
                .thenThrow(new BibliographicSourceException("All sources unavailable"));

        mockMvc.perform(get("/api/books/9789720706386/bibliographic"))
                .andExpect(status().isBadGateway());
    }
}
