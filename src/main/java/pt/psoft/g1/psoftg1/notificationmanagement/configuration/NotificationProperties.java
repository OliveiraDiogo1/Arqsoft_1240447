package pt.psoft.g1.psoftg1.notificationmanagement.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(
        @NotEmpty List<String> channels,
        @Valid @NotNull Email email,
        @Valid @NotNull Sms sms,
        @Valid @NotNull Webhook webhook) {

    public record Email(@jakarta.validation.constraints.Email String from) {}

    public record Sms(String url, String apiKey, String sender) {}

    public record Webhook(String url, String secret) {}
}
