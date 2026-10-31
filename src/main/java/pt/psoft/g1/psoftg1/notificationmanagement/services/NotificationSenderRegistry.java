package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.configuration.NotificationProperties;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationSender;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public final class NotificationSenderRegistry {

    private final Map<String, NotificationSender> sendersByChannel = new LinkedHashMap<>();
    private final List<NotificationSender> selected;

    public NotificationSenderRegistry(List<NotificationSender> senders, NotificationProperties properties) {
        for (NotificationSender sender : senders) {
            if (sendersByChannel.putIfAbsent(sender.channel(), sender) != null) {
                throw new IllegalArgumentException("Duplicate notification channel: " + sender.channel());
            }
        }
        Set<String> configuredChannels = new HashSet<>();
        for (String configured : properties.channels()) {
            if (!configuredChannels.add(configured)) {
                throw new IllegalArgumentException("Duplicate configured notification channel: " + configured);
            }
            if (!sendersByChannel.containsKey(configured)) {
                throw new IllegalArgumentException("Unknown notification channel: " + configured);
            }
        }
        this.selected = properties.channels().stream()
                .map(sendersByChannel::get)
                .toList();
        for (NotificationSender sender : this.selected) {
            sender.validateConfiguration();
        }
    }

    public List<NotificationSender> selected() {
        return selected;
    }
}
