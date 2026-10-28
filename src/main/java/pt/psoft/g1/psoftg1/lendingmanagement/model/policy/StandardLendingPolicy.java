package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;

public final class StandardLendingPolicy implements LendingPolicy {

    private final StandardPolicyConfig config;

    public StandardLendingPolicy(StandardPolicyConfig config) {
        this.config = config;
    }

    @Override
    public LendingPolicyType type() {
        return LendingPolicyType.STANDARD;
    }

    @Override
    public LendingTerms termsForNewLending(LendingPolicyContext context) {
        rejectOverdue(context);
        if (context.activeLendings() >= config.maxActiveLendings()) {
            throw new LendingForbiddenException("Reader has reached the maximum number of active lendings");
        }
        return new LendingTerms(config.lendingPeriodDays());
    }

    @Override
    public LendingTerms termsForRenewal(LendingPolicyContext context) {
        rejectOverdue(context);
        if (context.renewalCount() >= config.maxRenewals()) {
            throw new LendingForbiddenException("Lending has reached the maximum number of renewals");
        }
        if (context.activeLendings() > config.maxActiveLendings()) {
            throw new LendingForbiddenException("Reader has more active lendings than the policy allows");
        }
        return new LendingTerms(config.renewalPeriodDays());
    }

    private void rejectOverdue(LendingPolicyContext context) {
        if (context.hasOverdue()) {
            throw new LendingForbiddenException("Reader has overdue lending(s)");
        }
    }
}
