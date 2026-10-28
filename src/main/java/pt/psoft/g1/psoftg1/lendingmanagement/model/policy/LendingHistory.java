package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * Histórico do leitor usado para selecionar a política aplicável.
 */
public record LendingHistory(int completedLendings, int onTimeLendings, boolean hasOverdue) {

    public LendingHistory {
        if (completedLendings < 0) {
            throw new IllegalArgumentException("Completed lendings cannot be negative");
        }
        if (onTimeLendings < 0 || onTimeLendings > completedLendings) {
            throw new IllegalArgumentException("On-time lendings must be between zero and completed lendings");
        }
    }

    public double onTimeRatioPercent() {
        if (completedLendings == 0) {
            return 0.0;
        }
        return (onTimeLendings * 100.0) / completedLendings;
    }
}
