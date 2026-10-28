package pt.psoft.g1.psoftg1.lendingmanagement.api;

import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyType;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingPolicySnapshot;

public record LendingPolicyView(
        String readerNumber,
        LendingPolicyType policyType,
        int activeLendings,
        boolean hasOverdue,
        int completedLendings,
        int onTimeLendings,
        double onTimeRatioPercent,
        int applicablePeriodDays,
        int historyPeriodMonths,
        int minCompletedLendings,
        double minOnTimeRatioPercent) {

    public static LendingPolicyView from(LendingPolicySnapshot snapshot) {
        return new LendingPolicyView(
                snapshot.readerNumber(),
                snapshot.policyType(),
                snapshot.activeLendings(),
                snapshot.hasOverdue(),
                snapshot.completedLendings(),
                snapshot.onTimeLendings(),
                snapshot.onTimeRatioPercent(),
                snapshot.applicablePeriodDays(),
                snapshot.historyPeriodMonths(),
                snapshot.minCompletedLendings(),
                snapshot.minOnTimeRatioPercent());
    }
}
