package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationDeliveryException;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsNotificationSenderTest {

    @Test
    void postsSmsWithBearerTokenToConfiguredGateway() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://sms.example/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer key"))
                .andExpect(jsonPath("$.to").value("919191919"))
                .andExpect(jsonPath("$.sender").value("ARQSOFT"))
                .andExpect(jsonPath("$.message").value(containsString("2026/1")))
                .andRespond(withSuccess());
        var sender = new SmsNotificationSender(restTemplate,
                new NotificationProperties.Sms("https://sms.example/send", "key", "ARQSOFT"));

        sender.send(event());

        server.verify();
    }

    @Test
    void wrapsGatewayErrorsAsDeliveryFailures() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://sms.example/send"))
                .andRespond(withServerError());
        var sender = new SmsNotificationSender(restTemplate,
                new NotificationProperties.Sms("https://sms.example/send", "", "ARQSOFT"));

        assertThrows(NotificationDeliveryException.class, () -> sender.send(event()));
        server.verify();
    }

    @Test
    void requiresGatewayUrlWhenSmsChannelIsSelected() {
        var sender = new SmsNotificationSender(new RestTemplate(),
                new NotificationProperties.Sms(" ", "", "ARQSOFT"));

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
