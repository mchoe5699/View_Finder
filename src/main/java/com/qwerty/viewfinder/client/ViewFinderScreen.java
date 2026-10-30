package com.qwerty.viewfinder.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.qwerty.viewfinder.ViewFinderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 조작 키를 누르는 동안 열리는 투명 화면: 마우스 커서로 뷰 파인더를 조작한다. */
public class ViewFinderScreen extends Screen {
    public ViewFinderScreen() {
        super(Component.translatable("view_finder.screen"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int y = this.height - 62;
        g.drawCenteredString(this.font, Component.translatable("view_finder.hint.1"), this.width / 2, y, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("view_finder.hint.2"), this.width / 2, y + 11, 0xCCCCCC);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return ViewFinderController.mousePressed(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return ViewFinderController.mouseDragged(mx, my, button) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return ViewFinderController.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return ViewFinderController.mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    private static boolean holdMode() {
        return ViewFinderConfig.get(ViewFinderConfig.HOLD_TO_INTERACT, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!holdMode() && ViewFinderKeys.INTERACT.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (holdMode() && ViewFinderKeys.INTERACT.matches(keyCode, scanCode) && !ViewFinderController.isDragging()) {
            onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (holdMode() && !ViewFinderController.isDragging() && !isInteractKeyDown()) {
            onClose();
        }
    }

    private static boolean isInteractKeyDown() {
        InputConstants.Key key = ViewFinderKeys.INTERACT.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getValue() < 0) return false;
        return InputConstants.isKeyDown(window, key.getValue());
    }

    @Override
    public void removed() {
        ViewFinderController.cancelDrag();
        super.removed();
    }
}
