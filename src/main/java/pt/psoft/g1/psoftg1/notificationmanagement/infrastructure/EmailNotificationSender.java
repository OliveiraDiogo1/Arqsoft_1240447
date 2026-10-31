package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationDeliveryException;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationText;

@Component
public class EmailNotificationSender implements NotificationSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final NotificationProperties.Email properties;

    @Autowired
    public EmailNotificationSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   NotificationProperties properties) {
        this(mailSenderProvider, properties.email());
    }

    public EmailNotificationSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   NotificationProperties.Email properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @Override
    public String channel() {
        return "email";
    }

    @Override
    public void validateConfiguration() {
        if (properties.from() == null || properties.from().isBlank()) {
            throw new IllegalStateException("notifications.email.from is required when email is enabled");
        }
        if (mailSenderProvider.getIfAvailable() == null) {
            throw new IllegalStateException("JavaMailSender is required when email notifications are enabled");
        }
    }

    @Override
    public void send(LendingNotificationEvent event) {
        validateConfiguration();
        var message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(event.email());
        message.setSubject(NotificationText.subject(event));
        message.setText(NotificationText.emailBody(event));
        try {
            mailSenderProvider.getIfAvailable().send(message);
        } catch (MailException exception) {
            throw new NotificationDeliveryException("Email notification delivery failed", exception);
        }
    }
}
