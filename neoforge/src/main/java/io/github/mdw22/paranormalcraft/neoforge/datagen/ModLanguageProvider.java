package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
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
        addItem(ModItems.DEBUG_WISP, "Debug Wisp");
    }
}
