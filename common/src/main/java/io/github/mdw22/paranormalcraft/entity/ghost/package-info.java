/**
 * Everything ghosts share: {@link io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity} (base class), Presence,
 * visibility ({@code GhostVisibility}), passing through doors and panes ({@code GhostMovement}), cold spots and
 * generated names. New ghost types extend {@code GhostEntity}.
 * <p>
 * {@code Attachment} is a loose ghost following a player home (with {@code goal/GhostEscapeGoal} and
 * {@code goal/GhostFollowGoal}); Agitation lives on {@code GhostEntity}, and {@code AgitatedTint} is its colour.
 */
package io.github.mdw22.paranormalcraft.entity.ghost;
