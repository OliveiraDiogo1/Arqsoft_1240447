package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WebhookNotificationSenderTest {

    @Test
    void postsEventWithoutReaderContacts() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://hook.example/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Notification-Secret", "secret"))
                .andExpect(jsonPath("$.type").value("LENDING_CREATED"))
                .andExpect(jsonPath("$.lendingNumber").value("2026/1"))
                .andExpect(jsonPath("$.readerNumber").value("42"))
                .andExpect(jsonPath("$.isbn").value("9789720706386"))
                .andExpect(jsonPath("$.title").value("O Principe"))
                .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                .andExpect(jsonPath("$.limitDate").value("2026-01-16"))
                .andExpect(jsonPath("$.returnedDate").doesNotExist())
                .andExpect(content().string(not(containsString("reader@example.com"))))
                .andExpect(content().string(not(containsString("919191919"))))
                .andRespond(withSuccess());
        var sender = new WebhookNotificationSender(restTemplate,
                new NotificationProperties.Webhook("https://hook.example/events", "secret"));

        sender.send(event());

        server.verify();
    }

    @Test
    void requiresUrlWhenWebhookChannelIsSelected() {
        var sender = new WebhookNotificationSender(new RestTemplate(),
                new NotificationProperties.Webhook("", ""));

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
