package io.github.mdw22.paranormalcraft.haunting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HauntingPickerTest {

    private static List<HauntingPicker.Candidate> allReady() {

        List<HauntingPicker.Candidate> candidates = new ArrayList<>();
        for (HauntingAction action : HauntingAction.values()) {
            candidates.add(new HauntingPicker.Candidate(action, true, true));
        }
        return candidates;
    }

    @Test
    void weightsMatchTheDesign() {

        assertEquals(2, HauntingAction.KNOCK_OFF_DECORATION.weight());
        assertEquals(2, HauntingAction.SNUFF_LIGHT.weight());
        assertEquals(1, HauntingAction.FLIP_LEVER.weight());
        assertEquals(3, HauntingAction.TOGGLE_OPENING.weight());
        assertEquals(2, HauntingAction.PHANTOM_CHEST.weight());
        assertEquals(1, HauntingAction.TOSS_ITEMS.weight());
        assertEquals(3, HauntingAction.FOOTSTEPS.weight());
        assertEquals(2, HauntingAction.KNOCKING.weight());
    }

    @Test
    void onlyTheThreeBreakingActionsAreGriefing() {

        for (HauntingAction action : HauntingAction.values()) {
            boolean expected = action == HauntingAction.KNOCK_OFF_DECORATION || action == HauntingAction.SNUFF_LIGHT || action == HauntingAction.FLIP_LEVER;
            assertEquals(expected, action.griefing(), action.id());
        }
    }

    @Test
    void actionsWithoutTargetsAreFilteredOut() {

        List<HauntingPicker.Candidate> candidates = List.of(
                new HauntingPicker.Candidate(HauntingAction.KNOCKING, false, true),
                new HauntingPicker.Candidate(HauntingAction.FOOTSTEPS, true, true));

        assertEquals(List.of(HauntingAction.FOOTSTEPS), HauntingPicker.eligibleActions(candidates, true, false));
    }

    @Test
    void actionsOnCooldownAreFilteredOutUnlessForced() {

        List<HauntingPicker.Candidate> candidates = List.of(new HauntingPicker.Candidate(HauntingAction.KNOCKING, true, false));

        assertTrue(HauntingPicker.eligibleActions(candidates, true, false).isEmpty());
        assertEquals(List.of(HauntingAction.KNOCKING), HauntingPicker.eligibleActions(candidates, true, true));
    }

    @Test
    void griefingActionsNeedGriefingAllowedEvenWhenForced() {

        List<HauntingAction> eligible = HauntingPicker.eligibleActions(allReady(), false, true);

        assertFalse(eligible.contains(HauntingAction.KNOCK_OFF_DECORATION));
        assertFalse(eligible.contains(HauntingAction.SNUFF_LIGHT));
        assertFalse(eligible.contains(HauntingAction.FLIP_LEVER));
        assertEquals(5, eligible.size());
        assertEquals(8, HauntingPicker.eligibleActions(allReady(), true, false).size());
    }

    @Test
    void pickWalksTheWeights() {

        List<HauntingAction> eligible = List.of(HauntingAction.FLIP_LEVER, HauntingAction.FOOTSTEPS);
        // Total weight 4: roll 0 -> lever (weight 1), rolls 1..3 -> footsteps (weight 3).
        assertEquals(Optional.of(HauntingAction.FLIP_LEVER), HauntingPicker.pick(eligible, bound -> 0));
        for (int roll = 1; roll < 4; roll++) {
            int fixed = roll;
            assertEquals(Optional.of(HauntingAction.FOOTSTEPS), HauntingPicker.pick(eligible, bound -> fixed));
        }
        assertEquals(Optional.empty(), HauntingPicker.pick(List.of(), bound -> 0));
    }

    @Test
    void pickFrequenciesFollowTheWeights() {

        Random random = new Random(42);
        Map<HauntingAction, Integer> counts = new EnumMap<>(HauntingAction.class);
        List<HauntingAction> all = HauntingPicker.eligibleActions(allReady(), true, false);
        int samples = 160_000;
        for (int i = 0; i < samples; i++) {
            counts.merge(HauntingPicker.pick(all, random::nextInt).orElseThrow(), 1, Integer::sum);
        }
        int totalWeight = 16;
        for (HauntingAction action : HauntingAction.values()) {
            double expected = (double) action.weight() / totalWeight;
            double actual = counts.getOrDefault(action, 0) / (double) samples;
            assertEquals(expected, actual, 0.01, action.id());
        }
    }

    @Test
    void idsRoundTrip() {

        for (HauntingAction action : HauntingAction.values()) {
            assertEquals(Optional.of(action), HauntingAction.byId(action.id()));
        }
        assertEquals(Optional.empty(), HauntingAction.byId("possess_cat"));
    }
}
