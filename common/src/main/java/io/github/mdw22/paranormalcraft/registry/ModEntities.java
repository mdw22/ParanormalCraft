package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

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
