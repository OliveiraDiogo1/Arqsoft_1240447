package pt.psoft.g1.psoftg1.lendingmanagement.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.AdaptiveLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.AdaptivePolicyConfig;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.AdaptiveSelectionRule;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicyRegistry;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.SelectionConfig;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardDefaultRule;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardPolicyConfig;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(LendingPoliciesProperties.class)
public class LendingPolicyConfiguration {

    @Bean
    public StandardLendingPolicy standardLendingPolicy(LendingPoliciesProperties properties) {
        LendingPoliciesProperties.Standard standard = properties.standard();
        return new StandardLendingPolicy(new StandardPolicyConfig(
                standard.maxActiveLendings(),
                standard.lendingPeriodDays(),
                standard.maxRenewals(),
                standard.renewalPeriodDays()));
    }

    @Bean
    public AdaptiveLendingPolicy adaptiveLendingPolicy(LendingPoliciesProperties properties) {
        LendingPoliciesProperties.Adaptive adaptive = properties.adaptive();
        return new AdaptiveLendingPolicy(new AdaptivePolicyConfig(
                adaptive.maxActiveLendings(),
                adaptive.firstThreshold(),
                adaptive.secondThreshold(),
                adaptive.longPeriodDays(),
                adaptive.standardPeriodDays(),
                adaptive.shortPeriodDays(),
                adaptive.maxRenewals()));
    }

    @Bean
    public LendingPolicyRegistry lendingPolicyRegistry(List<LendingPolicy> policies) {
        return new LendingPolicyRegistry(policies);
    }

    @Bean
    public LendingPolicySelector lendingPolicySelector(LendingPoliciesProperties properties) {
        LendingPoliciesProperties.Selection selection = properties.selection();
        return new LendingPolicySelector(List.of(
                new AdaptiveSelectionRule(new SelectionConfig(
                        selection.historyPeriodMonths(),
                        selection.minCompletedLendings(),
                        selection.minOnTimeRatioPercent())),
                new StandardDefaultRule()));
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
