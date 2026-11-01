package pt.psoft.g1.psoftg1.lendingmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:arqsoft-lending-component;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class LendingComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @MockBean
    private NotificationService notificationService;

    private Librarian librarian;
    private ReaderDetails readerDetails;
    private Book book;

    @BeforeEach
    void setUp() {
        librarian = Librarian.newLibrarian("component.librarian@example.com", "Password123!", "Component Librarian");
        userRepository.save(librarian);

        Reader reader = Reader.newReader("component.reader@example.com", "Password123!", "Component Reader");
        userRepository.save(reader);
        readerDetails = readerRepository.save(new ReaderDetails(
                1, reader, "2000-01-01", "919191919", true, true, true, null, null));

        Author author = authorRepository.save(new Author("Component Author", "Biografia", null));
        Genre genre = genreRepository.save(new Genre("Component Genre"));
        book = bookRepository.save(new Book(
                "9782826012092", "Component Book", "Descrição", genre, List.of(author), null));
    }

    @Test
    void createsLendingThroughControllerServiceRepositoryAndDomain() throws Exception {
        mockMvc.perform(post("/api/lendings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn":"%s","readerNumber":"%s"}
                                """.formatted(book.getIsbn(), readerDetails.getReaderNumber()))
                        .with(jwt()
                                .jwt(token -> token.subject(librarian.getId() + "," + librarian.getUsername()))
                                .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookTitle").value("Component Book"))
                .andExpect(jsonPath("$.appliedPolicy").value("STANDARD"))
                .andExpect(jsonPath("$.appliedPeriodDays").value(15));
    }
}
