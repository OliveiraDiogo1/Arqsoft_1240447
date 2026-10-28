package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

public record StandardPolicyConfig(
        int maxActiveLendings,
        int lendingPeriodDays,
        int maxRenewals,
        int renewalPeriodDays) {

    public StandardPolicyConfig {
        if (maxActiveLendings <= 0) {
            throw new IllegalArgumentException("Max active lendings must be positive");
        }
        if (lendingPeriodDays <= 0) {
            throw new IllegalArgumentException("Lending period must be positive");
        }
        if (maxRenewals < 0) {
            throw new IllegalArgumentException("Max renewals cannot be negative");
        }
        if (renewalPeriodDays <= 0) {
            throw new IllegalArgumentException("Renewal period must be positive");
        }
    }
}
