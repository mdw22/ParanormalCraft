package io.github.mdw22.paranormalcraft.client;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.tool.EmfLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@code paranormalcraft:emf_level}: the local player's EMF level (1 to 5) for a reader they hold, and the baseline
 * 1 for anyone else's, so other players never see your reading.
 */
public record EmfLevelProperty() implements RangeSelectItemModelProperty {

    public static final Identifier ID = Constants.id("emf_level");
    public static final MapCodec<EmfLevelProperty> MAP_CODEC = MapCodec.unit(new EmfLevelProperty());

    @Override
    public float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {

        Minecraft minecraft = Minecraft.getInstance();
        boolean ownReader = owner != null && minecraft.player != null && owner.asLivingEntity() == minecraft.player;
        return ownReader ? ClientToolState.emfLevel() : EmfLevels.MIN_LEVEL;
    }

    @Override
    public MapCodec<EmfLevelProperty> type() {

        return MAP_CODEC;
    }
}
