package pt.psoft.g1.psoftg1.notificationmanagement.services;

import pt.psoft.g1.psoftg1.notificationmanagement.model.LendingNotificationEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

public final class NotificationText {

    private NotificationText() {}

    public static String subject(LendingNotificationEvent event) {
        return switch (event.type()) {
            case LENDING_CREATED -> "Empréstimo criado";
            case LENDING_RENEWED -> "Empréstimo renovado";
            case LENDING_RETURNED -> "Empréstimo devolvido";
        };
    }

    public static String emailBody(LendingNotificationEvent event) {
        var body = new StringBuilder()
                .append("Olá ").append(event.readerName()).append(",\n\n")
                .append(eventDescription(event.type())).append(".\n")
                .append("Empréstimo: ").append(event.lendingNumber()).append("\n")
                .append("Livro: ").append(event.bookTitle()).append("\n")
                .append("ISBN: ").append(event.isbn()).append("\n")
                .append("Data de início: ").append(event.startDate()).append("\n")
                .append("Data limite: ").append(event.limitDate());
        if (event.returnedDate() != null) {
            body.append("\nData de devolução: ").append(event.returnedDate());
        }
        return body.toString();
    }

    public static String smsBody(LendingNotificationEvent event) {
        var body = new StringBuilder()
                .append(eventDescription(event.type()))
                .append(" ").append(event.lendingNumber())
                .append(": ").append(event.bookTitle())
                .append(". Data limite: ").append(event.limitDate());
        if (event.returnedDate() != null) {
            body.append(". Devolvido em ").append(event.returnedDate());
        }
        return body.toString();
    }

    private static String eventDescription(NotificationEventType type) {
        return switch (type) {
            case LENDING_CREATED -> "O seu empréstimo foi criado";
            case LENDING_RENEWED -> "O seu empréstimo foi renovado";
            case LENDING_RETURNED -> "O seu empréstimo foi devolvido";
        };
    }
}
