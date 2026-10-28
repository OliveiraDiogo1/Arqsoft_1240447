package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardLendingPolicyTest {

    private final StandardLendingPolicy policy =
            new StandardLendingPolicy(new StandardPolicyConfig(3, 15, 2, 7));

    @Test
    void newLendingUsesConfiguredPeriodWhenReaderIsEligible() {
        LendingTerms terms = policy.termsForNewLending(new LendingPolicyContext(2, false, 0));

        assertEquals(15, terms.periodDays());
    }

    @Test
    void newLendingIsRejectedWhenReaderHasOverdueLending() {
        LendingPolicyContext context = new LendingPolicyContext(0, true, 0);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForNewLending(context));
    }

    @Test
    void newLendingIsRejectedAtMaximumActiveLendings() {
        LendingPolicyContext context = new LendingPolicyContext(3, false, 0);

        assertThrows(LendingForbiddenException.class,
                () -> policy.termsForNewLending(context));
    }

    @Test
    void renewalUsesConfiguredRenewalPeriod() {
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
