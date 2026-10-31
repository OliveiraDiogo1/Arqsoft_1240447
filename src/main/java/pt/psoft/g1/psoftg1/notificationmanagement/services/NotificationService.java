package pt.psoft.g1.psoftg1.notificationmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.notificationmanagement.model.NotificationEventType;

public interface NotificationService {
    void notify(Lending lending, NotificationEventType type);
}
