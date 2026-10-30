package com.qwerty.viewfinder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** 월드에 들어가면 자동으로 화면(기본: 맨 오른쪽 위)에 뷰 파인더를 그린다. */
public final class ViewFinderOverlay {
    private ViewFinderOverlay() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ViewFinderController.isEnabled()) return;
        // 인벤토리 화면에서는 어두운 배경 위에 따로 그린다 (ClientForgeEvents).
        if (ClientForgeEvents.isInventory(mc.screen)) return;
        boolean interacting = mc.screen instanceof ViewFinderScreen;
        if (mc.options.hideGui && !interacting) return;

        ViewFinderRenderer.Layout l = ViewFinderController.layout(screenWidth, screenHeight);
        float yaw = mc.player.getViewYRot(partialTick);
        float pitch = mc.player.getViewXRot(partialTick);

        ViewFinderGeometry.Face hover = null;
        if ((interacting || mc.screen instanceof ChatScreen) && !mc.mouseHandler.isMouseGrabbed()) {
            double mx = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();
            hover = ViewFinderController.hitAt(l, yaw, pitch, mx, my);
        }
        ViewFinderRenderer.draw(g, l, yaw, pitch, hover, interacting);
    }
}
