package pt.psoft.g1.psoftg1.notificationmanagement.model;

import java.time.LocalDate;

public record LendingNotificationEvent(
        NotificationEventType type,
        String lendingNumber,
        String readerNumber,
        String readerName,
        String email,
        String phoneNumber,
        String isbn,
        String bookTitle,
        LocalDate startDate,
        LocalDate limitDate,
        LocalDate returnedDate,
        boolean thirdPartySharingConsent) {
}
