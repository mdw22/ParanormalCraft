/**
 * Haunting: each ghost's {@code HauntingController} picks a weighted action ({@code HauntingAction},
 * {@code HauntingPicker}) on a timer ({@code HauntingSchedule}) and {@code HauntingEffects} carries it out; multi-tick effects
 * finish through {@code HauntingScheduler}, ticked from {@code CommonEvents}. Griefing actions need {@code mobGriefing} and
 * the {@code hauntingGriefing} config.
 */
package io.github.mdw22.paranormalcraft.haunting;
