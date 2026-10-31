package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

    @Test
    void sendsEmailToReader() {
        var mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        var sender = new EmailNotificationSender(provider,
                new NotificationProperties.Email("no-reply@example.com"));

        sender.send(event());

        var message = forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertArrayEquals(new String[]{"reader@example.com"}, message.getValue().getTo());
    }

    @Test
    void requiresFromAddressWhenEmailChannelIsSelected() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        var sender = new EmailNotificationSender(provider, new NotificationProperties.Email(" "));

        assertThrows(IllegalStateException.class, sender::validateConfiguration);
    }

    @Test
    void requiresMailSenderWhenEmailChannelIsSelected() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        var sender = new EmailNotificationSender(provider,
                new NotificationProperties.Email("no-reply@example.com"));

        assertThrows(IllegalStateException.class, sender::validateConfiguration);
    }

    private LendingNotificationEvent event() {
        return new LendingNotificationEvent(
                NotificationEventType.LENDING_CREATED,
                "2026/1",
                "42",
                "Reader",
                "reader@example.com",
                "919191919",
                "9789720706386",
                "O Principe",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 16),
                null,
                true);
    }
}
