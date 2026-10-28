package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

public record AdaptivePolicyConfig(
        int maxActiveLendings,
        int firstThreshold,
        int secondThreshold,
        int longPeriodDays,
        int standardPeriodDays,
        int shortPeriodDays,
        int maxRenewals) {

    public AdaptivePolicyConfig {
        if (maxActiveLendings <= 0) {
            throw new IllegalArgumentException("Max active lendings must be positive");
        }
        if (firstThreshold <= 0 || secondThreshold <= firstThreshold
                || secondThreshold >= maxActiveLendings) {
            throw new IllegalArgumentException(
                    "Thresholds must satisfy 0 < first < second < max active lendings");
        }
        if (longPeriodDays <= standardPeriodDays || standardPeriodDays <= shortPeriodDays
                || shortPeriodDays <= 0) {
            throw new IllegalArgumentException(
                    "Periods must satisfy long > standard > short > 0");
        }
        if (maxRenewals < 0) {
            throw new IllegalArgumentException("Max renewals cannot be negative");
        }
    }
}
