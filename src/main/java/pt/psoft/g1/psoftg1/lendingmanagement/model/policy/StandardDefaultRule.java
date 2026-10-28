package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Optional;

public final class StandardDefaultRule implements LendingPolicySelectionRule {

    @Override
    public Optional<LendingPolicyType> select(LendingHistory history) {
        return Optional.of(LendingPolicyType.STANDARD);
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
