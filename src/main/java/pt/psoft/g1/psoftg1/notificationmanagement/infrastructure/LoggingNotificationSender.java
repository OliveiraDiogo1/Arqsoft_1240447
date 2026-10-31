package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public String channel() {
        return "log";
    }

    @Override
    public void validateConfiguration() {}

    @Override
    public void send(LendingNotificationEvent event) {
        LOGGER.info("Lending notification {} for reader {} and lending {}",
                event.type(), event.readerNumber(), event.lendingNumber());
    }
}
