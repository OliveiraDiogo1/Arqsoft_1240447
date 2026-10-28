package pt.psoft.g1.psoftg1.lendingmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyType;

public record LendingPolicySnapshot(
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
}
