package com.qwerty.viewfinder.client;

import net.minecraft.network.chat.Component;

public final class ViewFinderText {
    private static final String[] DIR8 = {
            "north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"
    };

    private ViewFinderText() {}

    private static String horizontalKey(int dx, int dz) {
        if (dx == 0 && dz == 0) return null;
        if (dz < 0) return dx > 0 ? "northeast" : dx < 0 ? "northwest" : "north";
        if (dz > 0) return dx > 0 ? "southeast" : dx < 0 ? "southwest" : "south";
        return dx > 0 ? "east" : "west";
    }

    /** 면/모서리/꼭짓점이 가리키는 방향 이름 (예: 북동 · 위). */
    public static Component direction(ViewFinderGeometry.Face f) {
        String h = horizontalKey(f.dx, f.dz);
        Component v = f.dy > 0 ? Component.translatable("view_finder.dir.up")
                : f.dy < 0 ? Component.translatable("view_finder.dir.down") : null;
        if (h == null) return v;
        Component hc = Component.translatable("view_finder.dir." + h);
        if (v == null) return hc;
        return Component.translatable("view_finder.dir.combo", hc, v);
    }

    /** 현재 바라보는 방위와 높이 (예: 북동 45° · 아래 20°). */
    public static Component heading(float yaw, float pitch) {
        float bearing = ((yaw + 180f) % 360f + 360f) % 360f;
        int idx = Math.round(bearing / 45f) & 7;
        Component dir = Component.translatable("view_finder.dir." + DIR8[idx]);
        int deg = Math.round(bearing) % 360;
        int p = Math.round(pitch);
        Component pc;
        if (p == 0) pc = Component.translatable("view_finder.pitch.level");
        else if (p < 0) pc = Component.translatable("view_finder.pitch.up", -p);
        else pc = Component.translatable("view_finder.pitch.down", p);
        return Component.translatable("view_finder.heading", dir, deg, pc);
    }
}
