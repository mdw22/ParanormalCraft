package io.github.mdw22.paranormalcraft.haunting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * Chooses a haunting action. Plain logic so it can be unit tested: an action qualifies if it has a target, is off
 * cooldown (unless forced) and, for griefing actions, griefing is allowed; then one is picked by weight.
 */
public final class HauntingPicker {

    /**
     * One action's situation at the moment of picking.
     */
    public record Candidate(HauntingAction action, boolean hasTarget, boolean offCooldown) {
    }

    private HauntingPicker() {
    }

    public static boolean eligible(Candidate candidate, boolean griefingAllowed, boolean ignoreCooldowns) {

        return candidate.hasTarget()
                && (ignoreCooldowns || candidate.offCooldown())
                && (griefingAllowed || !candidate.action().griefing());
    }

    public static List<HauntingAction> eligibleActions(List<Candidate> candidates, boolean griefingAllowed, boolean ignoreCooldowns) {

        List<HauntingAction> eligible = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (eligible(candidate, griefingAllowed, ignoreCooldowns)) {
                eligible.add(candidate.action());
            }
        }
        return eligible;
    }

    /**
     * Weighted pick.
     *
     * @param nextInt returns a uniform int in [0, bound) for the given bound
     */
    public static Optional<HauntingAction> pick(List<HauntingAction> eligible, IntUnaryOperator nextInt) {

        int total = 0;
        for (HauntingAction action : eligible) {
            total += action.weight();
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = nextInt.applyAsInt(total);
        for (HauntingAction action : eligible) {
            roll -= action.weight();
            if (roll < 0) {
                return Optional.of(action);
            }
        }
        return Optional.of(eligible.getLast());
    }
}
