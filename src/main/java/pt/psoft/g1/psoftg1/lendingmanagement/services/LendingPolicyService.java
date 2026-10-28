package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.lendingmanagement.configuration.LendingPoliciesProperties;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyContext;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyRegistry;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyType;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingTerms;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LendingPolicyService {

    private final LendingRepository lendingRepository;
    private final LendingPolicySelector selector;
    private final LendingPolicyRegistry registry;
    private final LendingPoliciesProperties properties;
    private final Clock clock;

    public LendingPolicyDecision decideForNewLending(String readerNumber) {
        LocalDate today = LocalDate.now(clock);
        List<Lending> outstanding = lendingRepository.listOutstandingByReaderNumber(readerNumber);
        LendingHistory history = historyFor(readerNumber, outstanding, today);
        LendingPolicyType policyType = selector.select(history);
        LendingPolicy policy = registry.get(policyType);
        LendingPolicyContext context = new LendingPolicyContext(outstanding.size(), history.hasOverdue(), 0);
        LendingTerms terms = policy.termsForNewLending(context);
        return new LendingPolicyDecision(policyType, terms, history);
    }

    public LendingPolicyDecision decideForRenewal(Lending lending) {
        LocalDate today = LocalDate.now(clock);
        String readerNumber = lending.getReaderDetails().getReaderNumber();
        List<Lending> outstanding = lendingRepository.listOutstandingByReaderNumber(readerNumber);
        LendingHistory history = historyFor(readerNumber, outstanding, today);
        LendingPolicyType policyType = selector.select(history);
        LendingPolicy policy = registry.get(policyType);
        LendingPolicyContext context = new LendingPolicyContext(
                outstanding.size(), history.hasOverdue(), lending.getRenewalCount());
        LendingTerms terms = policy.termsForRenewal(context);
        return new LendingPolicyDecision(policyType, terms, history);
    }

    public LendingPolicySnapshot snapshot(String readerNumber) {
        LocalDate today = LocalDate.now(clock);
        List<Lending> outstanding = lendingRepository.listOutstandingByReaderNumber(readerNumber);
        LendingHistory history = historyFor(readerNumber, outstanding, today);
        LendingPolicyType policyType = selector.select(history);
        LendingPolicy policy = registry.get(policyType);
        int applicablePeriodDays = 0;
        try {
            applicablePeriodDays = policy.termsForNewLending(
                    new LendingPolicyContext(outstanding.size(), history.hasOverdue(), 0)).periodDays();
        } catch (LendingForbiddenException ignored) {
            // Zero significa que o leitor não é elegível para um novo empréstimo nesta política.
        }
        LendingPoliciesProperties.Selection selection = properties.selection();
        return new LendingPolicySnapshot(
                readerNumber,
                policyType,
                outstanding.size(),
                history.hasOverdue(),
                history.completedLendings(),
                history.onTimeLendings(),
                history.onTimeRatioPercent(),
                applicablePeriodDays,
                selection.historyPeriodMonths(),
                selection.minCompletedLendings(),
                selection.minOnTimeRatioPercent());
    }

    private LendingHistory historyFor(String readerNumber, List<Lending> outstanding, LocalDate today) {
        LocalDate since = today.minusMonths(properties.selection().historyPeriodMonths());
        List<Lending> returned = lendingRepository.listReturnedByReaderNumberSince(readerNumber, since);
        boolean hasOverdue = outstanding.stream().anyMatch(lending -> lending.isOverdue(today));
        int onTime = (int) returned.stream().filter(Lending::isOnTime).count();
        return new LendingHistory(returned.size(), onTime, hasOverdue);
    }
}
