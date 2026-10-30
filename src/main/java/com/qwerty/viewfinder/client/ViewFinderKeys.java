package com.qwerty.viewfinder.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ViewFinderKeys {
    public static final String CATEGORY = "key.categories.view_finder";

    public static final KeyMapping INTERACT = new KeyMapping("key.view_finder.interact",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    public static final KeyMapping TOGGLE = new KeyMapping("key.view_finder.toggle",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ViewFinderKeys() {}
}
