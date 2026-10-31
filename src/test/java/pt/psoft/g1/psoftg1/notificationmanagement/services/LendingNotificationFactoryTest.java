package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LendingNotificationFactoryTest {
    private final LendingNotificationFactory factory = new LendingNotificationFactory();
    private Lending lending;

    @BeforeEach
    void setUp() {
        Author author = new Author("Autor", "Biografia do autor", null);
        Book book = new Book("9789720706386", "O Príncipe", "Descrição do livro",
                new Genre("Ficção"), List.of(author), null);
        Reader reader = Reader.newReader("reader@example.com", "ReaderPassword123!", "Nome do Leitor");
        ReaderDetails readerDetails = new ReaderDetails(1, reader, "2000-01-01", "919191919",
                true, true, true, null, List.of());
        lending = Lending.newBootstrappingLending(book, readerDetails, 2026, 1,
                LocalDate.of(2026, 1, 1), null, 15, 0);
    }

    @Test
    void mapsReaderBookAndDates() {
        LendingNotificationEvent event = factory.create(lending, NotificationEventType.LENDING_CREATED);

        assertEquals(NotificationEventType.LENDING_CREATED, event.type());
        assertEquals("2026/1", event.lendingNumber());
        assertEquals("2026/1", event.readerNumber());
        assertEquals("Nome do Leitor", event.readerName());
        assertEquals("reader@example.com", event.email());
        assertEquals("919191919", event.phoneNumber());
        assertEquals("9789720706386", event.isbn());
        assertEquals("O Príncipe", event.bookTitle());
        assertEquals(LocalDate.of(2026, 1, 1), event.startDate());
        assertEquals(lending.getLimitDate(), event.limitDate());
        assertNull(event.returnedDate());
        assertTrue(event.thirdPartySharingConsent());
    }

    @Test
    void mapsReturnedDateForReturnEvent() {
        LocalDate returnedDate = LocalDate.of(2026, 1, 16);
        Lending returnedLending = Lending.newBootstrappingLending(lending.getBook(), lending.getReaderDetails(),
                2026, 1, LocalDate.of(2026, 1, 1), returnedDate, 15, 0);

        LendingNotificationEvent event = factory.create(returnedLending, NotificationEventType.LENDING_RETURNED);

        assertEquals(NotificationEventType.LENDING_RETURNED, event.type());
        assertEquals(returnedDate, event.returnedDate());
    }
}
