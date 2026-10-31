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

import java.util.LinkedHashMap;

@Component
public class WebhookNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate;
    private final NotificationProperties.Webhook properties;

    @Autowired
    public WebhookNotificationSender(RestTemplate restTemplate, NotificationProperties properties) {
        this(restTemplate, properties.webhook());
    }

    public WebhookNotificationSender(RestTemplate restTemplate, NotificationProperties.Webhook properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String channel() {
        return "webhook";
    }

    @Override
    public void validateConfiguration() {
        if (properties.url() == null || properties.url().isBlank()) {
            throw new IllegalStateException("notifications.webhook.url is required when webhook is enabled");
        }
    }

    @Override
    public void send(LendingNotificationEvent event) {
        validateConfiguration();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("type", event.type().name());
        payload.put("lendingNumber", event.lendingNumber());
        payload.put("readerNumber", event.readerNumber());
        payload.put("isbn", event.isbn());
        payload.put("title", event.bookTitle());
        payload.put("startDate", event.startDate().toString());
        payload.put("limitDate", event.limitDate().toString());
        if (event.returnedDate() != null) {
            payload.put("returnedDate", event.returnedDate().toString());
        }
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.secret() != null && !properties.secret().isBlank()) {
            headers.set("X-Notification-Secret", properties.secret());
        }
        try {
            restTemplate.exchange(properties.url(), HttpMethod.POST,
                    new HttpEntity<>(payload, headers), Void.class);
        } catch (RestClientException exception) {
            throw new NotificationDeliveryException("Webhook notification delivery failed", exception);
        }
    }
}
