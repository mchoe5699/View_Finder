package com.qwerty.viewfinder.client;

import com.qwerty.viewfinder.ViewFinderConfig;
import com.qwerty.viewfinder.ViewFinderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViewFinderMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ViewFinderKeys.TOGGLE.consumeClick()) {
            boolean on = !ViewFinderController.isEnabled();
            ViewFinderConfig.set(ViewFinderConfig.ENABLED, on);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable(on ? "view_finder.toggled.on" : "view_finder.toggled.off"), true);
            }
        }
        while (ViewFinderKeys.INTERACT.consumeClick()) {
            if (mc.screen == null && mc.player != null && ViewFinderController.isEnabled()) {
                mc.setScreen(new ViewFinderScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ViewFinderController.updateAnimation();
        }
    }

    // 채팅창(T)이 열려 커서가 보일 때도 뷰 파인더를 조작할 수 있게 한다.
    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof ChatScreen
                && ViewFinderController.mousePressed(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.getScreen() instanceof ChatScreen
                && ViewFinderController.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getScreen() instanceof ChatScreen
                && ViewFinderController.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof ChatScreen
                && ViewFinderController.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }
}
