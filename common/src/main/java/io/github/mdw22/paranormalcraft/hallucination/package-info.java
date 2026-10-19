/**
 * Server scheduling and pure placement maths for hallucinations: {@code HallucinationScheduler} (per-player timer,
 * ticked from {@code CommonEvents.onPlayerTick}) sends the request; {@code HallucinationPlacement}, {@code FigureRetry} and
 * {@code FigureSounds} are unit-tested rules the client uses to place and voice the shadow figure.
 */
package io.github.mdw22.paranormalcraft.hallucination;
