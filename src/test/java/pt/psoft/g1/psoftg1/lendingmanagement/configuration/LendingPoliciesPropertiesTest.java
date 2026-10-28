package pt.psoft.g1.psoftg1.lendingmanagement.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LendingPoliciesPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(validProperties());

    @Test
    void bindsValidConfiguration() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            LendingPoliciesProperties properties = context.getBean(LendingPoliciesProperties.class);
            assertThat(properties.standard().maxActiveLendings()).isEqualTo(3);
            assertThat(properties.standard().lendingPeriodDays()).isEqualTo(15);
            assertThat(properties.adaptive().firstThreshold()).isEqualTo(1);
            assertThat(properties.adaptive().secondThreshold()).isEqualTo(2);
            assertThat(properties.selection().minOnTimeRatioPercent()).isEqualTo(80.0);
        });
    }

    @Test
    void failsWhenAdaptiveThresholdsAreOutOfOrder() {
        runner.withPropertyValues(
                        "lending.policies.adaptive.first-threshold=3",
                        "lending.policies.adaptive.second-threshold=2")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWhenAdaptivePeriodsAreOutOfOrder() {
        runner.withPropertyValues(
                        "lending.policies.adaptive.long-period-days=10",
                        "lending.policies.adaptive.standard-period-days=14")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWhenOnTimeRatioIsOutsidePercentageRange() {
        runner.withPropertyValues("lending.policies.selection.min-on-time-ratio-percent=101")
                .run(context -> assertThat(context).hasFailed());
    }

    private static String[] validProperties() {
        return new String[]{
                "lending.policies.selection.history-period-months=12",
                "lending.policies.selection.min-completed-lendings=3",
                "lending.policies.selection.min-on-time-ratio-percent=80",
                "lending.policies.standard.max-active-lendings=3",
                "lending.policies.standard.lending-period-days=15",
                "lending.policies.standard.max-renewals=2",
                "lending.policies.standard.renewal-period-days=7",
                "lending.policies.adaptive.max-active-lendings=3",
                "lending.policies.adaptive.first-threshold=1",
                "lending.policies.adaptive.second-threshold=2",
                "lending.policies.adaptive.long-period-days=21",
                "lending.policies.adaptive.standard-period-days=14",
                "lending.policies.adaptive.short-period-days=7",
                "lending.policies.adaptive.max-renewals=2"
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LendingPoliciesProperties.class)
    static class TestConfiguration {
    }
}
