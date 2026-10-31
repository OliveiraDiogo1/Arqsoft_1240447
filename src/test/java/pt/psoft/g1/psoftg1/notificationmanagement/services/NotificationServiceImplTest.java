package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private final LendingNotificationFactory factory = new LendingNotificationFactory();
    private Lending lending;
    private Lending lendingWithoutConsent;

    @BeforeEach
    void setUp() {
        lending = lendingWithConsent(true);
        lendingWithoutConsent = lendingWithConsent(false);
    }

    @Test
    void sendsToAllSelectedChannels() {
        var email = fakeSender("email");
        var sms = fakeSender("sms");
        var webhook = fakeSender("webhook");
        var service = serviceWith(List.of(email, sms, webhook), List.of("email", "sms", "webhook"));

        service.notify(lending, NotificationEventType.LENDING_CREATED);

        assertTrue(email.sent);
        assertTrue(sms.sent);
        assertTrue(webhook.sent);
    }

    @Test
    void skipsWebhookWithoutThirdPartyConsent() {
        var webhook = fakeSender("webhook");
        var service = serviceWith(List.of(webhook), List.of("webhook"));

        service.notify(lendingWithoutConsent, NotificationEventType.LENDING_CREATED);

        assertFalse(webhook.sent);
    }

    @Test
    void senderFailureDoesNotPreventOtherChannels() {
        var broken = senderThatThrows("email");
        var sms = fakeSender("sms");
        var service = serviceWith(List.of(broken, sms), List.of("email", "sms"));

        assertDoesNotThrow(() -> service.notify(lending, NotificationEventType.LENDING_CREATED));
        assertTrue(sms.sent);
    }

    @Test
    void skipsEmailWithoutAddress() {
        var email = fakeSender("email");
        var mockedFactory = mock(LendingNotificationFactory.class);
        when(mockedFactory.create(lending, NotificationEventType.LENDING_CREATED))
                .thenReturn(event("", "919191919", true));
        var service = serviceWith(List.of(email), List.of("email"), mockedFactory);

        service.notify(lending, NotificationEventType.LENDING_CREATED);

        assertFalse(email.sent);
    }

    @Test
    void skipsSmsWithoutPhoneNumber() {
        var sms = fakeSender("sms");
        var mockedFactory = mock(LendingNotificationFactory.class);
        when(mockedFactory.create(lending, NotificationEventType.LENDING_CREATED))
                .thenReturn(event("reader@example.com", "", true));
        var service = serviceWith(List.of(sms), List.of("sms"), mockedFactory);

        service.notify(lending, NotificationEventType.LENDING_CREATED);

        assertFalse(sms.sent);
    }

    @Test
    void logChannelAlwaysSends() {
        var log = fakeSender("log");
        var mockedFactory = mock(LendingNotificationFactory.class);
        when(mockedFactory.create(lending, NotificationEventType.LENDING_CREATED))
                .thenReturn(event("", "", false));
        var service = serviceWith(List.of(log), List.of("log"), mockedFactory);

        service.notify(lending, NotificationEventType.LENDING_CREATED);

        assertTrue(log.sent);
    }

    @Test
    void eventCreationFailureDoesNotPropagate() {
        var mockedFactory = mock(LendingNotificationFactory.class);
        when(mockedFactory.create(lending, NotificationEventType.LENDING_CREATED))
                .thenThrow(new RuntimeException("boom"));
        var service = serviceWith(List.of(fakeSender("log")), List.of("log"), mockedFactory);

        assertDoesNotThrow(() -> service.notify(lending, NotificationEventType.LENDING_CREATED));
    }

    private NotificationServiceImpl serviceWith(List<NotificationSender> senders, List<String> channels) {
        return serviceWith(senders, channels, factory);
    }

    private NotificationServiceImpl serviceWith(List<NotificationSender> senders, List<String> channels,
                                                LendingNotificationFactory notificationFactory) {
        var properties = new NotificationProperties(channels,
                new NotificationProperties.Email("no-reply@example.com"),
                new NotificationProperties.Sms("https://sms.example.com", "api-key", "sender"),
                new NotificationProperties.Webhook("https://webhook.example.com", "secret"));
        var registry = new NotificationSenderRegistry(senders, properties);
        return new NotificationServiceImpl(notificationFactory, registry);
    }

    private FakeSender fakeSender(String channel) {
        return new FakeSender(channel);
    }

    private NotificationSender senderThatThrows(String channel) {
        return new NotificationSender() {
            @Override
            public String channel() {
                return channel;
            }

            @Override
            public void validateConfiguration() {
            }

            @Override
            public void send(LendingNotificationEvent event) {
                throw new RuntimeException("boom");
            }
        };
    }

    private Lending lendingWithConsent(boolean consent) {
        Author author = new Author("Autor", "Biografia do autor", null);
        Book book = new Book("9789720706386", "O Príncipe", "Descrição do livro",
                new Genre("Ficção"), List.of(author), null);
        Reader reader = Reader.newReader("reader@example.com", "ReaderPassword123!", "Nome do Leitor");
        ReaderDetails readerDetails = new ReaderDetails(1, reader, "2000-01-01", "919191919",
                true, true, consent, null, List.of());
        return Lending.newBootstrappingLending(book, readerDetails, 2026, 1,
                LocalDate.of(2026, 1, 1), null, 15, 0);
    }

    private LendingNotificationEvent event(String email, String phone, boolean consent) {
        return new LendingNotificationEvent(
                NotificationEventType.LENDING_CREATED,
                "2026/1",
                "1",
                "Nome do Leitor",
                email,
                phone,
                "9789720706386",
                "O Príncipe",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 16),
                null,
                consent);
    }

    private static final class FakeSender implements NotificationSender {
        private final String channel;
        private boolean sent;

        private FakeSender(String channel) {
            this.channel = channel;
        }

        @Override
        public String channel() {
            return channel;
        }

        @Override
        public void validateConfiguration() {
        }

        @Override
        public void send(LendingNotificationEvent event) {
            sent = true;
        }
    }
}
