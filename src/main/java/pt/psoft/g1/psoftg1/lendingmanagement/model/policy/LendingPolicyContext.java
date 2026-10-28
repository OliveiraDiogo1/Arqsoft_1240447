package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * Dados necessários para avaliar uma política num pedido de empréstimo ou renovação.
 */
public record LendingPolicyContext(int activeLendings, boolean hasOverdue, int renewalCount) {

    public LendingPolicyContext {
        if (activeLendings < 0) {
            throw new IllegalArgumentException("Active lendings cannot be negative");
        }
        if (renewalCount < 0) {
            throw new IllegalArgumentException("Renewal count cannot be negative");
        }
    }
}
