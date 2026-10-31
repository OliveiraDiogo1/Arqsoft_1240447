package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

@Component
public class LendingNotificationFactory {

    public LendingNotificationEvent create(Lending lending, NotificationEventType type) {
        var readerDetails = lending.getReaderDetails();
        var reader = readerDetails.getReader();
        var book = lending.getBook();

        return new LendingNotificationEvent(
                type,
                lending.getLendingNumber(),
                readerDetails.getReaderNumber(),
                reader.getName().toString(),
                reader.getUsername(),
                readerDetails.getPhoneNumber(),
                book.getIsbn(),
                book.getTitle().toString(),
                lending.getStartDate(),
                lending.getLimitDate(),
                lending.getReturnedDate(),
                readerDetails.isThirdPartySharingConsent());
    }
}
