package pt.psoft.g1.psoftg1.lendingmanagement.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lending.policies")
public record LendingPoliciesProperties(
        @Valid @NotNull Selection selection,
        @Valid @NotNull Standard standard,
        @Valid @NotNull Adaptive adaptive) {

    public record Selection(
            @Min(1) int historyPeriodMonths,
            @Min(1) int minCompletedLendings,
            @DecimalMin("0.0") @DecimalMax("100.0") double minOnTimeRatioPercent) {
    }

    public record Standard(
            @Min(1) int maxActiveLendings,
            @Min(1) int lendingPeriodDays,
            @Min(0) int maxRenewals,
            @Min(1) int renewalPeriodDays) {
    }

    public record Adaptive(
            @Min(1) int maxActiveLendings,
            @Min(1) int firstThreshold,
            @Min(1) int secondThreshold,
            @Min(1) int longPeriodDays,
            @Min(1) int standardPeriodDays,
            @Min(1) int shortPeriodDays,
            @Min(0) int maxRenewals) {
    }

    @AssertTrue(message = "Adaptive thresholds must satisfy 0 < first < second < max active lendings")
    public boolean isAdaptiveThresholdsValid() {
        return adaptive != null
                && adaptive.firstThreshold() < adaptive.secondThreshold()
                && adaptive.secondThreshold() < adaptive.maxActiveLendings();
    }

    @AssertTrue(message = "Adaptive periods must satisfy long > standard > short > 0")
    public boolean isAdaptivePeriodsValid() {
        return adaptive != null
                && adaptive.longPeriodDays() > adaptive.standardPeriodDays()
                && adaptive.standardPeriodDays() > adaptive.shortPeriodDays()
                && adaptive.shortPeriodDays() > 0;
    }
}
