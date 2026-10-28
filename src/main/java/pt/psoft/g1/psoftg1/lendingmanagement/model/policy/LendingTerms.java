package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * Resultado da aplicação de uma política: o número de dias do empréstimo.
 */
public record LendingTerms(int periodDays) {

    public LendingTerms {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("Lending period must be positive");
        }
    }
}
