package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LendingPolicySelectorTest {

    private final LendingPolicySelector selector = new LendingPolicySelector(List.of(
            new AdaptiveSelectionRule(new SelectionConfig(12, 3, 80.0)),
            new StandardDefaultRule()));

    @Test
    void selectsStandardWhenHistoryHasTooFewCompletedLendings() {
        LendingHistory history = new LendingHistory(2, 2, false);

        assertEquals(LendingPolicyType.STANDARD, selector.select(history));
    }

    @Test
    void selectsAdaptiveWhenAllConditionsAreMet() {
        LendingHistory history = new LendingHistory(3, 3, false);

        assertEquals(LendingPolicyType.ADAPTIVE, selector.select(history));
    }

    @Test
    void selectsAdaptiveAtExactOnTimeRatioBoundary() {
        LendingHistory history = new LendingHistory(5, 4, false);

        assertEquals(LendingPolicyType.ADAPTIVE, selector.select(history));
    }

    @Test
    void selectsStandardBelowOnTimeRatioBoundary() {
        LendingHistory history = new LendingHistory(5, 3, false);

        assertEquals(LendingPolicyType.STANDARD, selector.select(history));
    }

    @Test
    void selectsStandardWhenReaderHasOverdueLending() {
        LendingHistory history = new LendingHistory(5, 5, true);

        assertEquals(LendingPolicyType.STANDARD, selector.select(history));
    }

    @Test
    void usesRuleOrderToAllowExtensions() {
        LendingPolicySelectionRule forcedRule = new LendingPolicySelectionRule() {
            @Override
            public Optional<LendingPolicyType> select(LendingHistory history) {
                return Optional.of(LendingPolicyType.ADAPTIVE);
            }

            @Override
            public int order() {
                return 1;
            }
        };

        LendingPolicySelector orderedSelector = new LendingPolicySelector(List.of(
                new StandardDefaultRule(),
                new AdaptiveSelectionRule(new SelectionConfig(12, 3, 80.0)),
                forcedRule));

        assertEquals(LendingPolicyType.ADAPTIVE, orderedSelector.select(new LendingHistory(0, 0, true)));
    }
}
