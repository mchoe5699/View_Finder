package com.qwerty.viewfinder;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ViewFinderConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue SIZE;
    public static final ForgeConfigSpec.DoubleValue POS_X;
    public static final ForgeConfigSpec.DoubleValue POS_Y;
    public static final ForgeConfigSpec.DoubleValue DRAG_SENSITIVITY;
    public static final ForgeConfigSpec.IntValue ANIMATION_MS;
    public static final ForgeConfigSpec.DoubleValue OPACITY;
    public static final ForgeConfigSpec.BooleanValue HOLD_TO_INTERACT;
    public static final ForgeConfigSpec.BooleanValue SHOW_LABELS;
    public static final ForgeConfigSpec.BooleanValue SHOW_HEADING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("viewFinder");
        ENABLED = b.comment("Show the view finder on screen").define("enabled", true);
        SIZE = b.comment("Size of the view finder in GUI pixels").defineInRange("size", 64, 32, 192);
        POS_X = b.comment("Center X as a fraction of screen width (-1 = top-right corner)").defineInRange("posX", -1.0, -1.0, 1.0);
        POS_Y = b.comment("Center Y as a fraction of screen height (-1 = top-right corner)").defineInRange("posY", -1.0, -1.0, 1.0);
        DRAG_SENSITIVITY = b.comment("Rotation speed when dragging the view finder").defineInRange("dragSensitivity", 1.0, 0.1, 5.0);
        ANIMATION_MS = b.comment("Camera turn animation time in ms when clicking a face/edge/corner (0 = instant)").defineInRange("animationMs", 300, 0, 2000);
        OPACITY = b.comment("Opacity of the view finder").defineInRange("opacity", 0.9, 0.1, 1.0);
        HOLD_TO_INTERACT = b.comment("true: hold the interact key to use the mouse, false: press to toggle").define("holdToInteract", true);
        SHOW_LABELS = b.comment("Show direction labels on the faces").define("showLabels", true);
        SHOW_HEADING = b.comment("Show heading text under the view finder").define("showHeading", true);
        b.pop();
        SPEC = b.build();
    }

    private ViewFinderConfig() {}

    public static <T> T get(ForgeConfigSpec.ConfigValue<T> v, T fallback) {
        try {
            return v.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static <T> void set(ForgeConfigSpec.ConfigValue<T> v, T value) {
        try {
            v.set(value);
            v.save();
        } catch (Exception ignored) {
        }
    }
}
