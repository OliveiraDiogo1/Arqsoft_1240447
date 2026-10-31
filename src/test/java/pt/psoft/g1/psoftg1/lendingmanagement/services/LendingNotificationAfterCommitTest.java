package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
class LendingNotificationAfterCommitTest {

    @Autowired
    private LendingService lendingService;
    @Autowired
    private LendingRepository lendingRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockBean
    private NotificationService notificationService;

    private TransactionTemplate readCommittedTransaction;
    private Lending lending;
    private ReaderDetails readerDetails;
    private Reader reader;
    private Book book;
    private Author author;
    private Genre genre;

    @BeforeEach
    void setUp() {
        readCommittedTransaction = new TransactionTemplate(transactionManager);
        readCommittedTransaction.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        author = authorRepository.save(new Author("Manuel Antonio Pina", "Biografia", null));
        genre = genreRepository.save(new Genre("Género"));
        book = bookRepository.save(new Book("9782826012092", "O Inspetor Max", "Descrição",
                genre, List.of(author), null));
        reader = Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives");
        userRepository.save(reader);
        readerDetails = readerRepository.save(new ReaderDetails(1, reader, "2000-01-01", "919191919",
                true, true, true, null, null));

        doAnswer(invocation -> {
            Lending notified = invocation.getArgument(0);
            NotificationEventType type = invocation.getArgument(1);
            assertCommittedState(notified, type);
            return null;
        }).when(notificationService).notify(any(Lending.class), any(NotificationEventType.class));
    }

    @AfterEach
    void tearDown() {
        if (lending != null) {
            lendingRepository.delete(lending);
        }
        readerRepository.delete(readerDetails);
        userRepository.delete(reader);
        bookRepository.delete(book);
        genreRepository.delete(genre);
        authorRepository.delete(author);
    }

    @Test
    void notifiesCreatedRenewedAndReturnedOnlyAfterTheirChangesAreCommitted() {
        lending = lendingService.create(new CreateLendingRequest(book.getIsbn(), readerDetails.getReaderNumber()));
        lending = lendingService.renew(lending.getLendingNumber(), lending.getVersion());
        lending = lendingService.setReturned(lending.getLendingNumber(),
                new SetLendingReturnedRequest(null), lending.getVersion());

        verify(notificationService).notify(any(Lending.class), eq(NotificationEventType.LENDING_CREATED));
        verify(notificationService).notify(any(Lending.class), eq(NotificationEventType.LENDING_RENEWED));
        verify(notificationService).notify(any(Lending.class), eq(NotificationEventType.LENDING_RETURNED));
        verifyNoMoreInteractions(notificationService);
    }

    private void assertCommittedState(Lending notified, NotificationEventType type) {
        readCommittedTransaction.executeWithoutResult(status -> {
            var persisted = lendingRepository.findByLendingNumber(notified.getLendingNumber());
            assertThat(persisted).isPresent();
            if (type == NotificationEventType.LENDING_RENEWED) {
                assertThat(persisted.orElseThrow().getRenewalCount()).isEqualTo(1);
            }
            if (type == NotificationEventType.LENDING_RETURNED) {
                assertThat(persisted.orElseThrow().getReturnedDate()).isNotNull();
            }
        });
    }
}
