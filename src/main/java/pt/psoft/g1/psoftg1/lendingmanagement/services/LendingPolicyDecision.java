package pt.psoft.g1.psoftg1.lendingmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyType;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingTerms;

public record LendingPolicyDecision(
        LendingPolicyType policyType,
        LendingTerms terms,
        LendingHistory history) {
}
