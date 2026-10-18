package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {

        super(output, Constants.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {

        add("itemGroup." + Constants.MOD_ID, Constants.MOD_NAME);
        addItem(ModItems.SPECTRAL_GOGGLES, "Spectral Goggles");
        addItem(ModItems.LINGERING_SPIRIT_SPAWN_EGG, "Lingering Spirit Spawn Egg");
        addItem(ModItems.EMF_READER, "EMF Reader");
        addItem(ModItems.SPIRIT_THERMOMETER, "Spirit Thermometer");
        addEntityType(ModEntities.LINGERING_SPIRIT, "Lingering Spirit");

        add("subtitles.paranormalcraft.lingering_spirit.whisper", "Something whispers");
        add("subtitles.paranormalcraft.lingering_spirit.disperse", "Spirit disperses");
        add("subtitles.paranormalcraft.lingering_spirit.hurt", "Spirit wavers");
        add("subtitles.paranormalcraft.spectral_goggles.equip", "Goggles click into place");

        add("commands.paranormalcraft.reveal.on", "Ghost reveal on: every ghost is visible to you and home radii are drawn");
        add("commands.paranormalcraft.reveal.off", "Ghost reveal off");
        add("commands.paranormalcraft.debug.manifest", "Forced %s ghost(s) to manifest");
        add("commands.paranormalcraft.debug.presence", "Set Presence on %s ghost(s)");
        add("commands.paranormalcraft.debug.coldspot", "Showing the cold spot of %s ghost(s) for 10 seconds");
    }
}
