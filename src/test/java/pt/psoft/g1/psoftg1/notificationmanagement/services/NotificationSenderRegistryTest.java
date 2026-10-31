package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationSenderRegistryTest {

    @Test
    void duplicateConfiguredChannelFailsAtStartup() {
        var properties = new NotificationProperties(
                List.of("log", "log"),
                new NotificationProperties.Email("no-reply@example.com"),
                new NotificationProperties.Sms("", "", ""),
                new NotificationProperties.Webhook("", ""));

        assertThrows(IllegalArgumentException.class,
                () -> new NotificationSenderRegistry(List.of(new LogSender()), properties));
    }

    private static final class LogSender implements NotificationSender {
        @Override
        public String channel() {
            return "log";
        }

        @Override
        public void validateConfiguration() {
        }

        @Override
        public void send(LendingNotificationEvent event) {
        }
    }
}
