package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.entity.ThrownGhostTrap;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.familiar.WispFamiliarEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity type registrations and their default attributes: the Lingering Spirit, the client-only shadow figure and
 * the thrown Ghost Trap.
 */
public final class ModEntities {

    public static final RegistryEntry<EntityType<LingeringSpiritEntity>> LINGERING_SPIRIT = RegistrationHelper.entityType("lingering_spirit",
            () -> EntityType.Builder.of(LingeringSpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.3F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(8));

    /** Client-only hallucination; never spawned on a server, never saved, can't be summoned. */
    public static final RegistryEntry<EntityType<ShadowFigureEntity>> SHADOW_FIGURE = RegistrationHelper.entityType("shadow_figure",
            () -> EntityType.Builder.<ShadowFigureEntity>of(ShadowFigureEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.4F)
                    .noSave()
                    .noSummon()
                    .noLootTable()
                    .clientTrackingRange(0));

    /** A bound Lingering Spirit following its owner; never saved (recalled into its vessel instead). */
    public static final RegistryEntry<EntityType<WispFamiliarEntity>> WISP_FAMILIAR = RegistrationHelper.entityType("wisp_familiar",
            () -> EntityType.Builder.<WispFamiliarEntity>of(WispFamiliarEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .noSave()
                    .noSummon()
                    .noLootTable()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(1));

    /** A thrown Ghost Trap, arming where it lands. */
    public static final RegistryEntry<EntityType<ThrownGhostTrap>> THROWN_GHOST_TRAP = RegistrationHelper.entityType("thrown_ghost_trap",
            () -> EntityType.Builder.<ThrownGhostTrap>of(ThrownGhostTrap::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10));

    static {
        RegistrationHelper.entityAttributes(LINGERING_SPIRIT, GhostEntity::createGhostAttributes);
    }

    private ModEntities() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
