package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LendingPolicyRegistry {

    private final Map<LendingPolicyType, LendingPolicy> policies = new EnumMap<>(LendingPolicyType.class);

    public LendingPolicyRegistry(List<LendingPolicy> policies) {
        for (LendingPolicy policy : policies) {
            if (this.policies.put(policy.type(), policy) != null) {
                throw new IllegalArgumentException("Duplicate lending policy: " + policy.type());
            }
        }
    }

    public LendingPolicy get(LendingPolicyType type) {
        LendingPolicy policy = policies.get(type);
        if (policy == null) {
            throw new IllegalArgumentException("No lending policy registered for " + type);
        }
        return policy;
    }
}
