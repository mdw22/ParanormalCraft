/**
 * Milestone 2 capture: the Tether Wand's server logic ({@link io.github.mdw22.paranormalcraft.capture.TetherManager},
 * ticked from {@code CommonEvents}) and the pure, unit-tested rules behind it: Presence floor and Weakened
 * ({@code PresenceRules}), wand heat ({@code WandHeat}), beam geometry ({@code BeamMath}), trap capture
 * ({@code CaptureRules}), plus what a filled trap stores ({@code CapturedGhost}) and where filled traps are
 * ({@code FilledTraps}). Update 9 added tether steering ({@code Steering}), the trap deploy ({@code TrapDeploy}), the
empty-hand release hold ({@code ReleaseHold}) and {@code CapturedGhosts} for debug and tests.
 */
package io.github.mdw22.paranormalcraft.capture;
