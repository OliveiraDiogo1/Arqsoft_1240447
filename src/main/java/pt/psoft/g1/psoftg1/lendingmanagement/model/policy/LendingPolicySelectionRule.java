package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Optional;

public interface LendingPolicySelectionRule {

    Optional<LendingPolicyType> select(LendingHistory history);

    int order();
}
