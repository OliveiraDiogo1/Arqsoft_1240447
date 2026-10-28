package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Optional;

public final class AdaptiveSelectionRule implements LendingPolicySelectionRule {

    private final SelectionConfig config;

    public AdaptiveSelectionRule(SelectionConfig config) {
        this.config = config;
    }

    @Override
    public Optional<LendingPolicyType> select(LendingHistory history) {
        if (history.hasOverdue()) {
            return Optional.empty();
        }
        if (history.completedLendings() < config.minCompletedLendings()) {
            return Optional.empty();
        }
        if (history.onTimeRatioPercent() < config.minOnTimeRatioPercent()) {
            return Optional.empty();
        }
        return Optional.of(LendingPolicyType.ADAPTIVE);
    }

    @Override
    public int order() {
        return 10;
    }
}
