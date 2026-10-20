package io.github.mdw22.paranormalcraft;

/**
 * Every gameplay tuning number in one place: ranges, timers, thresholds, chances. The classes that use them keep
 * their own constant names as aliases (for example {@code PresenceRules.FLOOR = Balance.Presence.FLOOR}), so change
 * a value here and nowhere else. Config-file defaults ({@code ServerConfig.DEFAULTS}), loot ranges (datagen
 * {@code ModChestLoot}), haunting weights ({@code HauntingAction}) and the EMF distance table
 * ({@code EmfLevels.levelForDistance}) live where they are used.
 * <p>
 * Ticks are game ticks (20 a second); distances are blocks.
 */
public final class Balance {

    private Balance() {
    }

    /**
     * Ghost behaviour: ranges, manifests, spawning.
     */
    public static final class Ghost {

        /** Idle wandering stays within this many blocks of home. (Used as {@code GhostEntity.WANDER_RADIUS}.) */
        public static final int WANDER_RADIUS = 12;
        /** Beyond this the ghost drifts back home. (Used as {@code GhostEntity.LEASH_RADIUS}.) */
        public static final int LEASH_RADIUS = 16;
        /** How far players can be for curiosity and manifesting. (Used as {@code GhostEntity.PLAYER_RANGE}.) */
        public static final double PLAYER_RANGE = 10.0;
        /** See {@code travel}: top speed is {@code speed × 1.0 / (1 − 0.5)} blocks per tick. (Used as {@code GhostEntity.DRIFT_ACCELERATION}.) */
        public static final float DRIFT_ACCELERATION = 1.0F;
        /** (Used as {@code GhostEntity.DRIFT_DRAG}.) */
        public static final double DRIFT_DRAG = 0.5;
        /** 1.5 seconds. (Used as {@code GhostEntity.MANIFEST_TICKS}.) */
        public static final int MANIFEST_TICKS = 30;
        /** (Used as {@code GhostEntity.MANIFEST_RETRY_TICKS}.) */
        public static final int MANIFEST_RETRY_TICKS = 20;
        /** Vanilla's spectral arrow Glowing duration (its private default). (Used as {@code GhostEntity.SPECTRAL_ARROW_GLOWING_TICKS}.) */
        public static final int SPECTRAL_ARROW_GLOWING_TICKS = 200;
        /** Players within this distance hear "{@code <name>} has dispersed". (Used as {@code GhostEntity.DISPERSE_MESSAGE_RANGE}.) */
        public static final double DISPERSE_MESSAGE_RANGE = 32.0;
        /** (Used as {@code GhostMarkerBlockEntity.SECOND_GHOST_FACTOR}.) */
        public static final double SECOND_GHOST_FACTOR = 0.3;
        /** (Used as {@code GhostCuriousGoal.MIN_DISTANCE}.) */
        public static final double CURIOUS_MIN_DISTANCE = 4.0;
        /** (Used as {@code GhostCuriousGoal.MAX_DISTANCE}.) */
        public static final double CURIOUS_MAX_DISTANCE = 6.0;
        /** (Used as {@code GhostCuriousGoal.MIN_DURATION}.) */
        public static final int CURIOUS_MIN_TICKS = 100;
        /** (Used as {@code GhostCuriousGoal.MAX_DURATION}.) */
        public static final int CURIOUS_MAX_TICKS = 200;
        /** (Used as {@code GhostShyGoal.STARE_TICKS}.) */
        public static final int SHY_STARE_TICKS = 40;
        /** (Used as {@code GhostShyGoal.STARE_RANGE}.) */
        public static final double SHY_STARE_RANGE = 24.0;
        /** (Used as {@code GhostShyGoal.FLEE_TICKS}.) */
        public static final int SHY_FLEE_TICKS = 60;
        /** (Used as {@code GhostShyGoal.FLEE_DISTANCE}.) */
        public static final double SHY_FLEE_DISTANCE = 5.0;
        /** (Used as {@code GhostWanderGoal.COLD_SPOT_RADIUS}.) */
        public static final int WANDER_COLD_SPOT_RADIUS = 4;

        private Ghost() {
        }
    }

    /**
     * Presence (the ghost's health meter) and the Weakened state.
     */
    public static final class Presence {

        /** (Used as {@code Presence.MAX}.) */
        public static final float MAX = 20.0F;
        /** 10 seconds without a valid hit before regeneration starts. (Used as {@code Presence.REGEN_DELAY_TICKS}.) */
        public static final int REGEN_DELAY_TICKS = 200;
        /** Then 1 Presence every 2 seconds. (Used as {@code Presence.REGEN_INTERVAL_TICKS}.) */
        public static final int REGEN_INTERVAL_TICKS = 40;
        /** (Used as {@code Presence.REGEN_AMOUNT}.) */
        public static final float REGEN_AMOUNT = 1.0F;
        /** (Used as {@code PresenceRules.FLOOR}.) */
        public static final float FLOOR = 1.0F;
        /** (Used as {@code PresenceRules.MIN_MELEE_DRAIN}.) */
        public static final float MIN_MELEE_DRAIN = 0.5F;
        /** (Used as {@code PresenceRules.WEAKENED_AT}.) */
        public static final float WEAKENED_AT = 5.0F;
        /** 15 s. (Used as {@code PresenceRules.RECOVERY_TICKS}.) */
        public static final int RECOVERY_TICKS = 300;
        /** (Used as {@code PresenceRules.RECOVERED_PRESENCE}.) */
        public static final float RECOVERED_PRESENCE = 10.0F;
        /** A ghost that breaks free of a trap is left at this. (Used as {@code PresenceRules.BREAK_FREE_PRESENCE}.) */
        public static final float BREAK_FREE_PRESENCE = 8.0F;
        /** Each latched Tether drains this much per second (0.1 per tick). (Used as {@code PresenceRules.TETHER_DRAIN_PER_SECOND}.) */
        public static final float TETHER_DRAIN_PER_SECOND = 2.0F;

        private Presence() {
        }
    }

    /**
     * The Tether Wand: beam, pull, heat and backfire.
     */
    public static final class Tether {

        /** (Used as {@code TetherManager.RANGE}.) */
        public static final double RANGE = 12.0;
        /** (Used as {@code TetherManager.LATCH_DISTANCE}.) */
        public static final double LATCH_DISTANCE = 1.0;
        /** (Used as {@code TetherManager.BREAK_RANGE}.) */
        public static final double BREAK_RANGE = 14.0;
        /** (Used as {@code TetherManager.LOS_GRACE_TICKS}.) */
        public static final int LOS_GRACE_TICKS = 20;
        /** (Used as {@code TetherManager.BACKFIRE_DAMAGE}.) */
        public static final float BACKFIRE_DAMAGE = 4.0F;
        /** (Used as {@code TetherManager.BACKFIRE_KNOCKBACK}.) */
        public static final double BACKFIRE_KNOCKBACK = 0.9;
        /** Beams this close count as crossing. (Used as {@code BeamMath.CROSSING_DISTANCE}.) */
        public static final double CROSSING_DISTANCE = 0.4;
        /** The last stretch of each beam, where beams on the same target meet, never counts as a crossing. (Used as {@code BeamMath.TARGET_END_ZONE}.) */
        public static final double TARGET_END_ZONE = 1.5;
        /** (Used as {@code WandHeat.MAX_HEAT}.) */
        public static final int MAX_HEAT = 160;
        /** (Used as {@code WandHeat.COOL_PER_TICK}.) */
        public static final int COOL_PER_TICK = 2;
        /** (Used as {@code WandHeat.LOCKOUT_TICKS}.) */
        public static final int LOCKOUT_TICKS = 80;
        /** (Used as {@code WandHeat.TICKS_PER_DURABILITY}.) */
        public static final int TICKS_PER_DURABILITY = 20;
        /** Tether Wand durability: seconds of beam at one point per {@code TICKS_PER_DURABILITY}. (Used in {@code ModItems.TETHER_WAND}.) */
        public static final int DURABILITY = 300;

        // Steering (Update 9): the ghost follows a point in front of the holder, like a leash with a handle.
        /** On latching, the hold distance is the current distance clamped to this range. (Used in {@code TetherManager}.) */
        public static final double HOLD_MIN = 4.0;
        /** Upper limit of the hold distance, both on latching and when reeling out. (Used in {@code TetherManager}.) */
        public static final double HOLD_MAX = 10.0;
        /** Sneaking reels the hold distance in down to this. (Used in {@code TetherManager}.) */
        public static final double REEL_MIN = 3.0;
        /** Reeling in (sneak) or out (moving backward) changes the hold distance by this much a tick: 2 blocks a second. (Used in {@code TetherManager}.) */
        public static final double REEL_PER_TICK = 0.1;
        /** Fraction of the gap to the anchor the ghost closes each tick, with no fight in it. (Used in {@code Steering}.) */
        public static final double FOLLOW_RATE = 0.35;
        /** Top steering speed, blocks a tick (18 blocks a second): fast enough for a 90° sweep in about half a second. (Used in {@code Steering}.) */
        public static final double MAX_STEER_SPEED = 0.9;
        /** At full fight strength the follow rate is cut by this fraction. (Used in {@code Steering}.) */
        public static final double FIGHT_SLOWDOWN = 0.5;
        /** The ghost's jerks pull it up to this far off the beam line, at full fight strength. (Used in {@code Steering}.) */
        public static final double FIGHT_OFFSET_MAX = 1.5;
        /** Chance per tick of a new jerk (1 in this). (Used in {@code GhostEntity}.) */
        public static final int FIGHT_JERK_CHANCE = 8;
        /** Each tick a jerk's offset shrinks to this fraction, so it snaps back toward the line. (Used in {@code GhostEntity}.) */
        public static final double FIGHT_DECAY = 0.85;
        /** Fight strength of a Weakened ghost (full Presence fights at 1.0). (Used in {@code Steering}.) */
        public static final double WEAKENED_FIGHT = 0.25;
        /** Fight strength is multiplied by this when two or more tethers hold the ghost. (Used in {@code Steering}.) */
        public static final double COOP_FIGHT_FACTOR = 0.5;

        // Trap deploy (Update 9): attack while latched drops a trap under the ghost.
        /** Minimum ticks between two trap deploys by one player. (Used in {@code TetherManager}.) */
        public static final int DEPLOY_COOLDOWN_TICKS = 20;
        /** How far below the ghost a floor for the dropped trap is searched for. (Used in {@code TetherManager}.) */
        public static final int DEPLOY_FLOOR_SEARCH = 8;

        private Tether() {
        }
    }

    /**
     * The Ghost Trap and Filled Ghost Trap.
     */
    public static final class Trap {

        /** A capture pulls the ghost down for this long (1.5 s). (Used as {@code CaptureRules.CAPTURE_TICKS}.) */
        public static final int CAPTURE_TICKS = 30;
        /** Without a tether held for the whole pull, a capture succeeds this often. (Used as {@code CaptureRules.UNTETHERED_CHANCE}.) */
        public static final double UNTETHERED_CHANCE = 0.5;
        /** Holding use on a Filled Trap with an empty hand this long releases its ghost (Update 9). (Used in {@code ReleaseHold}.) */
        public static final int RELEASE_HOLD_TICKS = 40;
        /** A gap longer than this between use presses cancels the hold (the client repeats use every 4 ticks). (Used in {@code ReleaseHold}.) */
        public static final int RELEASE_GAP_TICKS = 6;
        /** The capture zone: this far above the trap's floor, and this far out horizontally from its centre. (Used as {@code CaptureRules.ZONE_HEIGHT}.) */
        public static final double ZONE_HEIGHT = 3.0;
        /** (Used as {@code CaptureRules.ZONE_HALF_WIDTH}.) */
        public static final double ZONE_HALF_WIDTH = 1.5;
        /** (Used as {@code FilledGhostTrapBlockEntity.MIN_RATTLE_TICKS}.) */
        public static final int MIN_RATTLE_TICKS = 200;
        /** (Used as {@code FilledGhostTrapBlockEntity.MAX_RATTLE_TICKS}.) */
        public static final int MAX_RATTLE_TICKS = 400;
        /** A trapped ghost reads at most this. (Used as {@code EmfReadings.TRAPPED_MAX_LEVEL}.) */
        public static final int TRAPPED_EMF_MAX_LEVEL = 3;
        /** (Used as {@code GhostTrapItem.THROW_SPEED}.) */
        public static final float THROW_SPEED = 0.8F;

        private Trap() {
        }
    }

    /**
     * Haunting actions (intervals are in the server config; action weights are in HauntingAction).
     */
    public static final class Haunting {

        /** Each action waits at least this long before it can repeat (60 s). (Used as {@code HauntingSchedule.ACTION_COOLDOWN_TICKS}.) */
        public static final long ACTION_COOLDOWN_TICKS = 1200;
        /** (Used as {@code HauntingController.PLAYER_RANGE}.) */
        public static final double PLAYER_RANGE = 16.0;
        /** (Used as {@code HauntingTargets.RANGE}.) */
        public static final int BLOCK_RANGE = 8;
        /** (Used as {@code HauntingTargets.ITEM_RANGE}.) */
        public static final int ITEM_RANGE = 6;
        /** A container only rattles if no player is this close (so nobody is about to open it). (Used as {@code HauntingTargets.CONTAINER_PLAYER_CLEARANCE}.) */
        public static final double CONTAINER_PLAYER_CLEARANCE = 4.0;
        /** The chest lid stays up this long (about 1.5 s). (Used as {@code HauntingEffects.CHEST_OPEN_TICKS}.) */
        public static final int CHEST_OPEN_TICKS = 30;
        /** Footsteps are spread over about 2 s. (Used as {@code HauntingEffects.FOOTSTEP_SPAN_TICKS}.) */
        public static final int FOOTSTEP_SPAN_TICKS = 40;
        /** (Used as {@code HauntingEffects.KNOCK_GAP_TICKS}.) */
        public static final int KNOCK_GAP_TICKS = 6;

        private Haunting() {
        }
    }

    /**
     * The shadow figure hallucination (the interval is in the server config).
     */
    public static final class Hallucination {

        /** (Used as {@code HallucinationScheduler.GHOST_RANGE}.) */
        public static final double GHOST_RANGE = 24.0;
        /** (Used as {@code HallucinationScheduler.MAX_BLOCK_LIGHT}.) */
        public static final int MAX_BLOCK_LIGHT = 7;
        /** (Used as {@code HallucinationScheduler.RETRY_TICKS}.) */
        public static final int RETRY_TICKS = 200;
        /** Never closer to the view than this, so the player has to turn a little before it vanishes. (Used as {@code HallucinationPlacement.MIN_ANGLE_FLOOR}.) */
        public static final double MIN_ANGLE_FLOOR = 28.0;
        /** The band's inner and outer edges, measured inward from the screen edge. (Used as {@code HallucinationPlacement.INNER_FROM_EDGE}.) */
        public static final double INNER_FROM_EDGE = 18.0;
        /** (Used as {@code HallucinationPlacement.OUTER_FROM_EDGE}.) */
        public static final double OUTER_FROM_EDGE = 6.0;
        /** (Used as {@code HallucinationPlacement.NEAR_MIN_DISTANCE}.) */
        public static final double NEAR_MIN_DISTANCE = 8.0;
        /** (Used as {@code HallucinationPlacement.NEAR_MAX_DISTANCE}.) */
        public static final double NEAR_MAX_DISTANCE = 13.0;
        /** {@code reduced} intensity keeps the figure further away. (Used as {@code HallucinationPlacement.REDUCED_MIN_DISTANCE}.) */
        public static final double REDUCED_MIN_DISTANCE = 12.0;
        /** (Used as {@code HallucinationPlacement.REDUCED_MAX_DISTANCE}.) */
        public static final double REDUCED_MAX_DISTANCE = 16.0;
        /** The figure vanishes once the player's view comes this close to it. (Used as {@code HallucinationPlacement.VANISH_VIEW_ANGLE}.) */
        public static final double VANISH_VIEW_ANGLE = 20.0;
        /** (Used as {@code FigureRetry.RETRY_INTERVAL}.) */
        public static final int RETRY_INTERVAL = 10;
        /** 2 s of failed tries indoors before falling back to sound first. (Used as {@code FigureRetry.SOUND_FIRST_AFTER}.) */
        public static final int SOUND_FIRST_AFTER = 40;
        /** 10 s, after which the request is dropped. (Used as {@code FigureRetry.GIVE_UP_TICKS}.) */
        public static final int GIVE_UP_TICKS = 200;
        /** Sound-first spots: this many blocks out. (Used as {@code FigureRetry.SOUND_FIRST_MIN_DISTANCE}.) */
        public static final double SOUND_FIRST_MIN_DISTANCE = 6.0;
        /** (Used as {@code FigureRetry.SOUND_FIRST_MAX_DISTANCE}.) */
        public static final double SOUND_FIRST_MAX_DISTANCE = 13.0;
        /** Indoors, a visible spot inside (the next room) may be this close. (Used as {@code FigureRetry.INDOOR_MIN_DISTANCE}.) */
        public static final double INDOOR_MIN_DISTANCE = 3.0;
        /** (Used as {@code FigureSounds.FULL_MIN_INTERVAL}.) */
        public static final int SOUND_FULL_MIN_INTERVAL = 20;
        /** (Used as {@code FigureSounds.FULL_MAX_INTERVAL}.) */
        public static final int SOUND_FULL_MAX_INTERVAL = 30;
        /** (Used as {@code FigureSounds.REDUCED_INTERVAL}.) */
        public static final int SOUND_REDUCED_INTERVAL = 40;
        /** (Used as {@code FigureSounds.FULL_VOLUME}.) */
        public static final float SOUND_FULL_VOLUME = 0.6F;
        /** (Used as {@code FigureSounds.REDUCED_VOLUME}.) */
        public static final float SOUND_REDUCED_VOLUME = 0.3F;
        /** (Used as {@code ShadowFigureEntity.LIFETIME_TICKS}.) */
        public static final int FIGURE_LIFETIME_TICKS = 200;

        private Hallucination() {
        }
    }

    /**
     * The EMF Reader and Spirit Thermometer (the EMF distance table is in EmfLevels.levelForDistance).
     */
    public static final class Tools {

        /** Ghosts further than this don't register; the reader shows its baseline light. (Used as {@code EmfLevels.RANGE}.) */
        public static final double EMF_RANGE = 24.0;
        /** (Used as {@code ToolTracker.INTERVAL_TICKS}.) */
        public static final int READING_INTERVAL_TICKS = 10;
        /** How long ghost activity pins nearby readers at level 5. (Used as {@code ToolTracker.SPIKE_TICKS}.) */
        public static final int EMF_SPIKE_TICKS = 60;
        /** EMF holders within this many blocks of ghost activity spike to level 5. (Used as {@code GhostEvents.ACTIVITY_RANGE}.) */
        public static final double EMF_SPIKE_RANGE = 16.0;
        /** (Used as {@code Temperature.DEGREES_PER_BIOME_UNIT}.) */
        public static final double DEGREES_PER_BIOME_UNIT = 20.0;
        /** (Used as {@code Temperature.NIGHT_OFFSET}.) */
        public static final double NIGHT_OFFSET = -3.0;
        /** (Used as {@code Temperature.BLOCKS_PER_DEGREE_OF_ALTITUDE}.) */
        public static final double BLOCKS_PER_DEGREE_OF_ALTITUDE = 16.0;
        /** (Used as {@code Temperature.COLD_SPOT_STRENGTH}.) */
        public static final double COLD_SPOT_STRENGTH = 10.0;
        /** (Used as {@code Temperature.COLD_SPOT_RADIUS}.) */
        public static final double COLD_SPOT_RADIUS = 4.0;
        /** (Used as {@code Temperature.NOISE}.) */
        public static final double THERMOMETER_NOISE = 0.3;
        /** Below this the holder's breath shows. (Used as {@code Temperature.BREATH_BELOW}.) */
        public static final double BREATH_BELOW = 3.0;
        /** Display bands: blue below, orange above. (Used as {@code Temperature.COLD_BELOW}.) */
        public static final double COLD_BELOW = 5.0;
        /** (Used as {@code Temperature.WARM_ABOVE}.) */
        public static final double WARM_ABOVE = 25.0;

        private Tools() {
        }
    }

    /**
     * The Containment Unit: storage, charge, processing and release (per-type outputs are in ContainmentTables).
     */
    public static final class Containment {

        /** Ghosts one unit holds. Upgrades may raise it later. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int CAPACITY = 4;
        /** The charge meter's top. (Used in {@code ContainmentCharge}.) */
        public static final int MAX_CHARGE = 100;
        /** Charge one soul sand or soul soil adds. (Used in {@code ContainmentCharge}.) */
        public static final int FUEL_CHARGE = 20;
        /** Fuel is consumed automatically while the charge is at or below this. (Used in {@code ContainmentCharge}.) */
        public static final int REFUEL_AT_OR_BELOW = 80;
        /** Each stored ghost drains 1 charge per this many ticks (30 s): 4 ghosts drain 100 in 12.5 minutes. (Used in {@code ContainmentCharge}.) */
        public static final int DRAIN_TICKS_PER_GHOST = 600;
        /** Processing one ghost takes this many ticks (5 in-game minutes). (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int PROCESS_TICKS = 6000;
        /** Processing drains 1 extra charge per this many ticks (1 a minute). (Used in {@code ContainmentCharge}.) */
        public static final int PROCESS_DRAIN_TICKS = 1200;
        /** Chance a processed ghost also leaves a Spectral Residue. (Used in {@code ContainmentTables}.) */
        public static final double RESIDUE_CHANCE = 0.1;
        /** Lingering Spirit: Lingering Ectoplasm per processed ghost, inclusive range. (Used in {@code ContainmentTables}.) */
        public static final int LINGERING_YIELD_MIN = 3;
        public static final int LINGERING_YIELD_MAX = 5;
        /** Lingering Spirit's blessing, Restful: 3 in-game days. (Used in {@code ContainmentTables}.) */
        public static final int RESTFUL_TICKS = 72_000;
        /** A released ghost rises and fades for this long. (Used in {@code GhostEntity}.) */
        public static final int ASCEND_TICKS = 60;
        /** Blocks per tick a released ghost rises. (Used in {@code GhostEntity}.) */
        public static final double ASCEND_SPEED = 0.05;
        /** The Unstable alarm repeats this often. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int ALARM_INTERVAL_TICKS = 60;
        /** The ambient hum repeats this often while ghosts are stored. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int HUM_INTERVAL_TICKS = 80;
        /** Processing bubbles repeat this often. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int BUBBLE_INTERVAL_TICKS = 40;

        private Containment() {
        }
    }

    /**
     * The Ward Lantern (Update 9). The radius is the server config {@code wardLanternRadius}; this is its default.
     */
    public static final class Ward {

        /** Default ward radius, blocks. (Used in {@code ServerConfig.DEFAULTS}.) */
        public static final int DEFAULT_RADIUS = 32;
        /** Light level of the lantern. (Used in {@code ModBlocks.WARD_LANTERN}.) */
        public static final int LIGHT = 12;
        /** Light level while flaring. (Used in {@code ModBlocks.WARD_LANTERN}.) */
        public static final int FLARE_LIGHT = 15;
        /** How long a flare lasts. (Used in {@code WardEffects}.) */
        public static final int FLARE_TICKS = 10;
        /** At most one flare per lantern per this many ticks. (Used in {@code WardEffects}.) */
        public static final int FLARE_COOLDOWN_TICKS = 60;
        /** A ghost the ward caught recoils about this far from the lantern. (Used in {@code WardEffects}.) */
        public static final double RECOIL_DISTANCE = 2.0;
        /** The placement ring is traced over this many ticks. (Used in {@code WardEffects}.) */
        public static final int RING_TICKS = 60;
        /** At most one denied-spawn puff per lantern per this many ticks. (Used in {@code WardEffects}.) */
        public static final int PUFF_COOLDOWN_TICKS = 40;
        /** Denied-spawn puffs only show within this distance of a player. (Used in {@code WardEffects}.) */
        public static final double PUFF_PLAYER_RANGE = 32.0;
        /** Ghosts don't pick wander or curious targets this close to a lantern. (Used in {@code HoverSpots, GhostCuriousGoal}.) */
        public static final double KEEP_AWAY = 4.0;
        /** Holding a Ward Lantern shows the boundaries of wards within this range. (Used in {@code ClientWardPreview}.) */
        public static final double PREVIEW_RANGE = 48.0;
        /** Preview motes are drawn on the edge stretch within this distance of the player. (Used in {@code ClientWardPreview}.) */
        public static final double PREVIEW_EDGE_RANGE = 24.0;
        /** Preview motes per ward per round. (Used in {@code ClientWardPreview}.) */
        public static final int PREVIEW_MOTES = 6;
        /** A preview round every this many ticks. (Used in {@code ClientWardPreview}.) */
        public static final int PREVIEW_INTERVAL_TICKS = 4;

        private Ward() {
        }
    }

    /**
     * Binding a stored ghost into a Spirit Vessel (Update 10). Which job each ghost type gets is in {@code BoundJobs}.
     */
    public static final class Binding {

        /** Binding takes this long (60 s). (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int BIND_TICKS = 1200;
        /** Charge one bind costs, taken when it starts. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int BIND_CHARGE = 5;
        /** The rising bind tone plays this often. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final int TONE_INTERVAL_TICKS = 20;
        /** The tone's pitch at the start of a bind. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final float TONE_PITCH_START = 0.5F;
        /** The tone's pitch at the end of a bind. (Used in {@code ContainmentUnitBlockEntity}.) */
        public static final float TONE_PITCH_END = 2.0F;

        private Binding() {
        }
    }

    /**
     * The Wisp Familiar, a bound Lingering Spirit (Update 10).
     */
    public static final class Familiar {

        /** Nearest follow distance from the owner, blocks; it sits midway. (Used in {@code WispFamiliarEntity}.) */
        public static final double FOLLOW_MIN = 2.0;
        /** Farthest follow distance from the owner, blocks. (Used in {@code WispFamiliarEntity}.) */
        public static final double FOLLOW_MAX = 3.0;
        /** Height above the owner's feet the wisp keeps (about shoulder height). (Used in {@code WispFamiliarEntity}.) */
        public static final double SHOULDER_HEIGHT = 1.5;
        /** Past this distance it teleports to its owner, like a tamed pet. (Used in {@code WispFamiliarEntity}.) */
        public static final double TELEPORT_DISTANCE = 16.0;
        /** Fraction of the way to its target it moves each tick. (Used in {@code WispFamiliarEntity}.) */
        public static final double FOLLOW_RATE = 0.12;
        /** Render scale against the Lingering Spirit. (Used in {@code WispFamiliarRenderer}.) */
        public static final float SCALE = 0.4F;
        /** Level of the light block it carries. (Used in {@code WispLight}.) */
        public static final int LIGHT_LEVEL = 11;
        /** It carries a light only where the light around it, without its own, is at or below this. (Used in {@code WispLight}.) */
        public static final int DARK_AT_OR_BELOW = 7;
        /** Hostiles within this range of the owner can be warned about. (Used in {@code WispWarnings}.) */
        public static final double WARN_RANGE = 12.0;
        /** A mob more than this many degrees from the owner's view counts as unseen. (Used in {@code WispWarnings}.) */
        public static final double WARN_ANGLE = 100.0;
        /** At most one chirp per mob per this many ticks. (Used in {@code WispFamiliarEntity}.) */
        public static final int WARN_COOLDOWN_TICKS = 200;
        /** Threats are re-checked this often. (Used in {@code WispFamiliarEntity}.) */
        public static final int WARN_SCAN_TICKS = 5;
        /** While warning, it drifts this far toward the threat's side of the owner. (Used in {@code WispFamiliarEntity}.) */
        public static final double WARN_DRIFT = 2.5;
        /** Guide: search radius for an Abandoned Cabin, chunks. (Used in {@code Familiars}.) */
        public static final int GUIDE_RADIUS_CHUNKS = 64;
        /** Guide: how far ahead of the owner it goes and waits. (Used in {@code WispFamiliarEntity}.) */
        public static final double GUIDE_AHEAD = 8.0;
        /** Guide: how long it waits there. (Used in {@code WispFamiliarEntity}.) */
        public static final int GUIDE_WAIT_TICKS = 400;
        /** Guide cooldown after a successful guide (2 minutes). (Used in {@code Familiars}.) */
        public static final int GUIDE_COOLDOWN_TICKS = 2400;
        /** Guide cooldown after "no cabin in range", so a player can't spam the search. (Used in {@code Familiars}.) */
        public static final int GUIDE_FAIL_COOLDOWN_TICKS = 200;
        /** Listed light blocks no wisp owns are swept this often, in loaded chunks. (Used in {@code Familiars}.) */
        public static final int LIGHT_SWEEP_TICKS = 100;
        /** How long the "no" shake lasts. (Used in {@code WispFamiliarEntity}.) */
        public static final int SHAKE_TICKS = 30;

        private Familiar() {
        }
    }

    /**
     * The Spectral Step charm (Update 10).
     */
    public static final class Phase {

        /** A phase lasts this long (5 s). (Used in {@code Phasing}.) */
        public static final int PHASE_TICKS = 100;
        /** Cooldown after use (60 s). (Used in {@code SpectralStepItem}.) */
        public static final int COOLDOWN_TICKS = 1200;
        /** Uses before it breaks; repairable with Lingering Ectoplasm. (Used in {@code ModItems.SPECTRAL_STEP}.) */
        public static final int USES = 20;
        /** A phase that ends inside a block is extended by up to this much. (Used in {@code Phasing}.) */
        public static final int MAX_EXTEND_TICKS = 60;
        /** If still stuck, the player is nudged to a free spot within this many blocks. (Used in {@code Phasing}.) */
        public static final int NUDGE_RADIUS = 3;
        /** The phased state goes to players within this many blocks. (Used in {@code Phasing}.) */
        public static final double SYNC_RANGE = 128.0;
        /** The phased state is re-sent to nearby players this often, for anyone who came into range. (Used in {@code Phasing}.) */
        public static final int RESYNC_TICKS = 10;

        private Phase() {
        }
    }

    /**
     * The Field Journal (Update 9).
     */
    public static final class Journal {

        /** Most capture log entries kept per player; the oldest go first. (Used in {@code CaptureLog}.) */
        public static final int CAPTURE_LOG_MAX = 100;
        /** A ghost counts as "seen" by a goggle wearer within this range who is looking at it. (Used in {@code JournalEvents}.) */
        public static final double SEEN_RANGE = 16.0;
        /** "Detected" needs EMF level 5 with the nearest ghost within this range. (Used in {@code JournalEvents}.) */
        public static final double DETECTED_RANGE = 5.0;
        /** Cold Feet: a thermometer reading below this at a cold spot. (Used in {@code JournalEvents}.) */
        public static final double COLD_FEET_BELOW = 5.0;

        private Journal() {
        }
    }

    /**
     * Worldgen: how often Abandoned Cabins generate.
     */
    public static final class World {

        /** (Used as {@code ModStructures.SPACING}.) */
        public static final int CABIN_SPACING = 32;
        /** (Used as {@code ModStructures.SEPARATION}.) */
        public static final int CABIN_SEPARATION = 12;

        private World() {
        }
    }
}
