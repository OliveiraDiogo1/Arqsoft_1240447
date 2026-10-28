package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class LendingPolicySelector {

    private final List<LendingPolicySelectionRule> rules;

    public LendingPolicySelector(List<LendingPolicySelectionRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(LendingPolicySelectionRule::order))
                .toList();
    }

    public LendingPolicyType select(LendingHistory history) {
        return rules.stream()
                .map(rule -> rule.select(history))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No lending policy selection rule matched"));
    }
}
