package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

public interface LendingPolicy {

    LendingPolicyType type();

    LendingTerms termsForNewLending(LendingPolicyContext context);

    LendingTerms termsForRenewal(LendingPolicyContext context);
}
