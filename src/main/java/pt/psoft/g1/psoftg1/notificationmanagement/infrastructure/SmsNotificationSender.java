package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationDeliveryException;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationText;

import java.util.Map;

@Component
public class SmsNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate;
    private final NotificationProperties.Sms properties;

    @Autowired
    public SmsNotificationSender(RestTemplate restTemplate, NotificationProperties properties) {
        this(restTemplate, properties.sms());
    }

    public SmsNotificationSender(RestTemplate restTemplate, NotificationProperties.Sms properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String channel() {
        return "sms";
    }

    @Override
    public void validateConfiguration() {
        if (properties.url() == null || properties.url().isBlank()) {
            throw new IllegalStateException("notifications.sms.url is required when SMS is enabled");
        }
    }

    @Override
    public void send(LendingNotificationEvent event) {
        validateConfiguration();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            headers.setBearerAuth(properties.apiKey());
        }
        var body = Map.of(
                "to", event.phoneNumber(),
                "sender", properties.sender(),
                "message", NotificationText.smsBody(event));
        try {
            restTemplate.exchange(properties.url(), HttpMethod.POST,
                    new HttpEntity<>(body, headers), Void.class);
        } catch (RestClientException exception) {
            throw new NotificationDeliveryException("SMS notification delivery failed", exception);
        }
    }
}
