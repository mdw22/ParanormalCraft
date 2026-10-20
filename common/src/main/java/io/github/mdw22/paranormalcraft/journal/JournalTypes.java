package io.github.mdw22.paranormalcraft.journal;

import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEffects;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What the Field Journal shows for each ghost type: the entity for the portrait, the small icon for the capture log,
 * and the facts rows under the portrait (each unlocked at a stage). The stage notes are lang
 * ({@code journal.paranormalcraft.<type path>.stage1..4} and {@code .bound}). A new ghost type adds one entry.
 */
public final class JournalTypes {

    /**
     * An icon in a facts row: an item, or a mob effect's sprite. {@code tooltip} overrides the item or effect name.
     */
    public record Icon(@Nullable Supplier<ItemStack> item, @Nullable Supplier<Holder<MobEffect>> effect, @Nullable Component tooltip) {

        public static Icon item(Supplier<? extends Item> item) {

            return new Icon(() -> new ItemStack(item.get()), null, null);
        }

        public static Icon stack(Supplier<ItemStack> stack, Component tooltip) {

            return new Icon(stack, null, tooltip);
        }

        public static Icon effect(Supplier<Holder<MobEffect>> effect) {

            return new Icon(null, effect, null);
        }

        public ItemStack stack() {

            return this.item == null ? ItemStack.EMPTY : this.item.get();
        }

        /** The hover text: the override, else the item's or effect's name. */
        public Component name() {

            if (this.tooltip != null) {
                return this.tooltip;
            }
            if (this.effect != null) {
                return this.effect.get().value().getDisplayName();
            }
            return stack().getHoverName();
        }
    }

    /**
     * A facts row: a label ({@code journal.paranormalcraft.row.<key>}), its icons, an optional short note after them,
     * and the stage that unlocks it.
     */
    public record Fact(String key, int unlockStage, List<Icon> icons, @Nullable Component note) {
    }

    /**
     * One ghost type's page data.
     */
    public record TypeInfo(Supplier<? extends EntityType<? extends GhostEntity>> entity, Supplier<? extends Item> logIcon, List<Fact> facts) {
    }

    private static final Map<String, TypeInfo> TYPES = Map.of(
            "paranormalcraft:lingering_spirit", new TypeInfo(ModEntities.LINGERING_SPIRIT, ModItems.LINGERING_SPIRIT_SPAWN_EGG, List.of(
                    new Fact("found_with", 1, List.of(Icon.item(ModItems.EMF_READER), Icon.item(ModItems.SPIRIT_THERMOMETER),
                            Icon.item(ModItems.SPECTRAL_GOGGLES)), null),
                    new Fact("calmed_by", 3, List.of(Icon.item(ModItems.WARD_LANTERN)), null),
                    new Fact("process", 4, List.of(Icon.item(ModItems.LINGERING_ECTOPLASM)),
                            Component.translatable("journal.paranormalcraft.note.yield", 3, 5)),
                    new Fact("release", 4, List.of(Icon.effect(() -> RegistrationHelper.holder(ModEffects.RESTFUL))),
                            Component.translatable("journal.paranormalcraft.note.nights", 3)),
                    new Fact("bind", 4, List.of(Icon.stack(JournalTypes::boundVessel, Component.translatable("job.paranormalcraft.wisp_familiar.icon"))),
                            Component.translatable("job.paranormalcraft.wisp_familiar")))));

    private JournalTypes() {
    }

    /** A bound-looking vessel for the icon (its ghost is a blank placeholder). */
    private static ItemStack boundVessel() {

        ItemStack stack = new ItemStack(ModItems.SPIRIT_VESSEL.get());
        stack.set(ModDataComponents.CAPTURED_GHOST.get(), new CapturedGhost("paranormalcraft:lingering_spirit", new CompoundTag(), "", 0L, "", "",
                Optional.empty()));
        return stack;
    }

    public static @Nullable TypeInfo of(String ghostType) {

        return TYPES.get(ghostType);
    }
}
