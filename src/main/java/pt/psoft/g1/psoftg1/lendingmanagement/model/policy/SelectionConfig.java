package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

public record SelectionConfig(
        int historyPeriodMonths,
        int minCompletedLendings,
        double minOnTimeRatioPercent) {

    public SelectionConfig {
        if (historyPeriodMonths <= 0) {
            throw new IllegalArgumentException("History period must be positive");
        }
        if (minCompletedLendings <= 0) {
            throw new IllegalArgumentException("Minimum completed lendings must be positive");
        }
        if (minOnTimeRatioPercent < 0.0 || minOnTimeRatioPercent > 100.0) {
            throw new IllegalArgumentException("Minimum on-time ratio must be between 0 and 100");
        }
    }
}
