package pt.psoft.g1.psoftg1.notificationmanagement.model;

public interface NotificationSender {
    String channel();

    void validateConfiguration();

    void send(LendingNotificationEvent event);
}
