package io.github.mdw22.paranormalcraft;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    public static final String MOD_ID = "paranormalcraft";
    public static final String MOD_NAME = "Paranormal Craft";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Creates an identifier in the mod's namespace, e.g. {@code paranormalcraft:debug_wisp}.
     */
    public static Identifier id(String path) {

        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
