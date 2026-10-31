package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final LendingNotificationFactory factory;
    private final NotificationSenderRegistry registry;

    public NotificationServiceImpl(LendingNotificationFactory factory, NotificationSenderRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    @Override
    public void notify(Lending lending, NotificationEventType type) {
        final LendingNotificationEvent event;
        try {
            event = factory.create(lending, type);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to build notification event of type {}", type, exception);
            return;
        }

        for (var sender : registry.selected()) {
            try {
                if (shouldSkip(sender.channel(), event)) {
                    continue;
                }
                sender.send(event);
            } catch (RuntimeException exception) {
                LOGGER.warn("Notification via {} failed for lending {} ({})",
                        sender.channel(), event.lendingNumber(), exception.getClass().getName());
            }
        }
    }

    private boolean shouldSkip(String channel, LendingNotificationEvent event) {
        return switch (channel) {
            case "email" -> isBlank(event.email());
            case "sms" -> isBlank(event.phoneNumber());
            case "webhook" -> !event.thirdPartySharingConsent();
            default -> false;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
