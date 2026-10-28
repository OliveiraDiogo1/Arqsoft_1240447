package pt.psoft.g1.psoftg1.lendingmanagement.model;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyType;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@PropertySource({"classpath:config/library.properties"})
class LendingTest {
    private static final int LENDING_DURATION_DAYS = 15;
    private static final ArrayList<Author> authors = new ArrayList<>();
    private static Book book;
    private static ReaderDetails readerDetails;
    @Value("${fineValuePerDayInCents}")
    private int fineValuePerDayInCents;

    @BeforeAll
    public static void setup(){
        Author author = new Author("Manuel Antonio Pina",
                "Manuel António Pina foi um jornalista e escritor português, premiado em 2011 com o Prémio Camões",
                null);
        authors.add(author);
        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária, vai ser fundamental para resolver um importante caso de uma rede de malfeitores que quer colocar uma bomba num megaconcerto de uma ilustre cantora",
                new Genre("Romance"),
                authors,
                null);
        readerDetails = new ReaderDetails(1,
                Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01",
                "919191919",
                true,
                true,
                true,
                null,
                null);
    }

    @Test
    void ensureBookNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(null, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents));
    }

    @Test
    void ensureReaderNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, null, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents));
    }

    @Test
    void ensureValidReaderNumber(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, readerDetails, -1, LENDING_DURATION_DAYS, fineValuePerDayInCents));
    }

    @Test
    void testSetReturned(){
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        lending.setReturned(0,null);
        assertEquals(LocalDate.now(), lending.getReturnedDate());
    }

    @Test
    void testGetDaysDelayed(){
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(0, lending.getDaysDelayed());
    }

    @Test
    void testGetDaysUntilReturn(){
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(Optional.of(LENDING_DURATION_DAYS), lending.getDaysUntilReturn());
    }

    @Test
    void testGetDaysOverDue(){
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(Optional.empty(), lending.getDaysOverdue());
    }

    @Test
    void testGetTitle() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals("O Inspetor Max", lending.getTitle());
    }

    @Test
    void testGetLendingNumber() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(LocalDate.now().getYear() + "/1", lending.getLendingNumber());
    }

    @Test
    void testGetBook() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(book, lending.getBook());
    }

    @Test
    void testGetReaderDetails() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(readerDetails, lending.getReaderDetails());
    }

    @Test
    void testGetStartDate() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(LocalDate.now(), lending.getStartDate());
    }

    @Test
    void testGetLimitDate() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertEquals(LocalDate.now().plusDays(LENDING_DURATION_DAYS), lending.getLimitDate());
    }

    @Test
    void testGetReturnedDate() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        assertNull(lending.getReturnedDate());
    }

    @Test
    void renewExtendsLimitDateAndUpdatesPolicySnapshot() {
        LocalDate startDate = LocalDate.of(2026, 1, 10);
        Lending lending = new Lending(book, readerDetails, 1, startDate, 15, fineValuePerDayInCents,
                LendingPolicyType.STANDARD);

        lending.renew(7, 0, LendingPolicyType.ADAPTIVE);

        assertEquals(1, lending.getRenewalCount());
        assertEquals(startDate.plusDays(22), lending.getLimitDate());
        assertEquals(LendingPolicyType.ADAPTIVE, lending.getAppliedPolicy());
        assertEquals(7, lending.getAppliedPeriodDays());
    }

    @Test
    void renewRejectsReturnedLending() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);
        lending.setReturned(0, null);

        assertThrows(IllegalArgumentException.class,
                () -> lending.renew(7, 0, LendingPolicyType.STANDARD));
    }

    @Test
    void renewRejectsStaleVersion() {
        Lending lending = new Lending(book, readerDetails, 1, LENDING_DURATION_DAYS, fineValuePerDayInCents);

        assertThrows(StaleObjectStateException.class,
                () -> lending.renew(7, -1, LendingPolicyType.STANDARD));
    }

    @Test
    void isOverdueOnlyAppliesToOutstandingLendingsPastLimitDate() {
        LocalDate today = LocalDate.of(2026, 2, 1);
        Lending overdue = new Lending(book, readerDetails, 1, today.minusDays(10), 5,
                fineValuePerDayInCents, LendingPolicyType.STANDARD);
        Lending onTime = new Lending(book, readerDetails, 2, today.minusDays(2), 5,
                fineValuePerDayInCents, LendingPolicyType.STANDARD);

        assertTrue(overdue.isOverdue(today));
        assertFalse(onTime.isOverdue(today));
    }

    @Test
    void isOnTimeComparesReturnDateWithLimitDate() {
        Lending onTime = Lending.newBootstrappingLending(book, readerDetails, 2026, 1,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20), 15,
                fineValuePerDayInCents);
        Lending late = Lending.newBootstrappingLending(book, readerDetails, 2026, 2,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 30), 15,
                fineValuePerDayInCents);

        assertTrue(onTime.isOnTime());
        assertFalse(late.isOnTime());
    }

}
