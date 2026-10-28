package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptiveLendingPolicyTest {

    private final AdaptiveLendingPolicy policy = new AdaptiveLendingPolicy(
            new AdaptivePolicyConfig(3, 1, 2, 21, 14, 7, 2));

    @Test
    void newLendingBelowFirstThresholdUsesLongPeriod() {
        LendingTerms terms = policy.termsForNewLending(new LendingPolicyContext(0, false, 0));

        assertEquals(21, terms.periodDays());
    }

    @Test
    void newLendingAtFirstThresholdUsesStandardPeriod() {
        LendingTerms terms = policy.termsForNewLending(new LendingPolicyContext(1, false, 0));

        assertEquals(14, terms.periodDays());
    }

    @Test
    void newLendingAtSecondThresholdUsesShortPeriod() {
        LendingTerms terms = policy.termsForNewLending(new LendingPolicyContext(2, false, 0));

        assertEquals(7, terms.periodDays());
    }

    @Test
    void newLendingIsRejectedAtMaximumActiveLendings() {
        LendingPolicyContext context = new LendingPolicyContext(3, false, 0);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForNewLending(context));
    }

    @Test
    void newLendingIsRejectedWhenReaderHasOverdueLending() {
        LendingPolicyContext context = new LendingPolicyContext(0, true, 0);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForNewLending(context));
    }

    @Test
    void renewalRecomputesPeriodFromCurrentActiveLendings() {
        assertEquals(21, policy.termsForRenewal(new LendingPolicyContext(0, false, 0)).periodDays());
        assertEquals(14, policy.termsForRenewal(new LendingPolicyContext(1, false, 0)).periodDays());
        assertEquals(7, policy.termsForRenewal(new LendingPolicyContext(2, false, 0)).periodDays());
    }

    @Test
    void renewalIsAllowedAtMaximumActiveLendings() {
        LendingTerms terms = policy.termsForRenewal(new LendingPolicyContext(3, false, 0));

        assertEquals(7, terms.periodDays());
    }

    @Test
    void renewalIsRejectedAtMaximumRenewals() {
        LendingPolicyContext context = new LendingPolicyContext(1, false, 2);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForRenewal(context));
    }

    @Test
    void renewalIsRejectedWhenReaderHasOverdueLending() {
        LendingPolicyContext context = new LendingPolicyContext(1, true, 0);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForRenewal(context));
    }
}
