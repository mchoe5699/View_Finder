package com.qwerty.viewfinder.client;

import com.qwerty.viewfinder.ViewFinderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.List;

/** 뷰 파인더의 위치·마우스 조작·시점 애니메이션 상태. */
public final class ViewFinderController {
    private enum Drag { NONE, PENDING, ROTATE, MOVE }

    private static final int MARGIN = 6;
    private static final double DRAG_THRESHOLD = 3.0;

    private static Drag drag = Drag.NONE;
    private static int button;
    private static double pressX, pressY, lastX, lastY, grabDX, grabDY;
    private static boolean moveOnly;
    private static ViewFinderGeometry.Face pressedFace;
    private static Integer moveX, moveY;

    private static boolean animating;
    private static float startYaw, startPitch, targetYaw, targetPitch;
    private static long animStart;
    private static int animMs;

    private ViewFinderController() {}

    public static boolean isEnabled() {
        return ViewFinderConfig.get(ViewFinderConfig.ENABLED, true);
    }

    public static boolean isDragging() {
        return drag != Drag.NONE;
    }

    public static boolean isMoving() {
        return drag == Drag.MOVE;
    }

    private static int screenW() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private static int screenH() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static ViewFinderRenderer.Layout layout() {
        return layout(screenW(), screenH());
    }

    public static ViewFinderRenderer.Layout layout(int sw, int sh) {
        int size = ViewFinderConfig.get(ViewFinderConfig.SIZE, 64);
        int x, y;
        if (moveX != null) {
            x = moveX;
            y = moveY;
        } else {
            double fx = ViewFinderConfig.get(ViewFinderConfig.POS_X, -1.0);
            double fy = ViewFinderConfig.get(ViewFinderConfig.POS_Y, -1.0);
            if (fx < 0 || fy < 0) {
                x = sw - size - MARGIN;
                y = MARGIN;
            } else {
                x = (int) Math.round(fx * sw - size / 2.0);
                y = (int) Math.round(fy * sh - size / 2.0);
            }
        }
        x = Mth.clamp(x, 0, Math.max(0, sw - size));
        y = Mth.clamp(y, 0, Math.max(0, sh - size - 10));
        return new ViewFinderRenderer.Layout(x, y, size);
    }

    public static ViewFinderGeometry.Face hitAt(ViewFinderRenderer.Layout l, float yaw, float pitch, double mx, double my) {
        if (!l.contains(mx, my)) return null;
        List<ViewFinderRenderer.Proj> faces = ViewFinderRenderer.project(yaw, pitch, l);
        return ViewFinderRenderer.hit(faces, mx, my);
    }

    // ---------------------------------------------------------------- 마우스

    /** 뷰 파인더 조작 화면(Alt)·채팅창: 클릭 = 그 방향 보기, 드래그 = 시점 회전. */
    public static boolean mousePressed(double mx, double my, int btn) {
        return mousePressed(mx, my, btn, false);
    }

    /** moveOnly = true (인벤토리): 클릭 = 그 방향 보기, 끌기 = 위치 이동. */
    public static boolean mousePressed(double mx, double my, int btn, boolean moveOnly) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isEnabled()) return false;
        ViewFinderRenderer.Layout l = layout();
        if (!l.contains(mx, my)) return false;
        if (btn == 2) {
            resetPosition();
            return true;
        }
        if (moveOnly ? (btn != 0 && btn != 1) : btn != 0) return false;
        ViewFinderController.moveOnly = moveOnly;
        drag = Drag.PENDING;
        button = btn;
        pressX = lastX = mx;
        pressY = lastY = my;
        grabDX = mx - l.x();
        grabDY = my - l.y();
        pressedFace = hitAt(l, player.getYRot(), player.getXRot(), mx, my);
        return true;
    }

    public static boolean mouseDragged(double mx, double my, int btn) {
        if (drag == Drag.NONE) return false;
        if (drag == Drag.PENDING) {
            double dx = mx - pressX, dy = my - pressY;
            if (dx * dx + dy * dy < DRAG_THRESHOLD * DRAG_THRESHOLD) return true;
            drag = moveOnly ? Drag.MOVE : Drag.ROTATE;
        }
        if (drag == Drag.ROTATE) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                int size = ViewFinderConfig.get(ViewFinderConfig.SIZE, 64);
                float k = 90f / size * ViewFinderConfig.get(ViewFinderConfig.DRAG_SENSITIVITY, 1.0).floatValue();
                // 잡고 끄는 느낌: 오른쪽으로 끌면 왼쪽 면이 앞으로 온다(왼쪽으로 돌아봄).
                float yaw = player.getYRot() - (float) (mx - lastX) * k;
                float pitch = player.getXRot() - (float) (my - lastY) * k;
                animating = false;
                setRotation(player, yaw, pitch);
            }
        } else if (drag == Drag.MOVE) {
            int size = ViewFinderConfig.get(ViewFinderConfig.SIZE, 64);
            int sw = screenW(), sh = screenH();
            moveX = Mth.clamp((int) Math.round(mx - grabDX), 0, Math.max(0, sw - size));
            moveY = Mth.clamp((int) Math.round(my - grabDY), 0, Math.max(0, sh - size - 10));
        }
        lastX = mx;
        lastY = my;
        return true;
    }

    public static boolean mouseReleased(double mx, double my, int btn) {
        if (drag == Drag.NONE) return false;
        if (drag == Drag.PENDING && button == 0 && pressedFace != null) {
            lookAt(pressedFace);
        } else if (drag == Drag.MOVE) {
            savePosition();
        }
        drag = Drag.NONE;
        pressedFace = null;
        return true;
    }

    public static boolean mouseScrolled(double mx, double my, double delta) {
        if (!isEnabled() || delta == 0) return false;
        ViewFinderRenderer.Layout l = layout();
        if (!l.contains(mx, my)) return false;
        int size = Mth.clamp(l.size() + (delta > 0 ? 4 : -4), 32, 192);
        ViewFinderConfig.set(ViewFinderConfig.SIZE, size);
        return true;
    }

    public static void cancelDrag() {
        if (drag == Drag.MOVE) savePosition();
        drag = Drag.NONE;
        pressedFace = null;
    }

    private static void savePosition() {
        if (moveX == null) return;
        int size = ViewFinderConfig.get(ViewFinderConfig.SIZE, 64);
        int sw = screenW(), sh = screenH();
        double fx = Mth.clamp((moveX + size / 2.0) / sw, 0.0, 1.0);
        double fy = Mth.clamp((moveY + size / 2.0) / sh, 0.0, 1.0);
        ViewFinderConfig.set(ViewFinderConfig.POS_X, fx);
        ViewFinderConfig.set(ViewFinderConfig.POS_Y, fy);
        moveX = null;
        moveY = null;
    }

    private static void resetPosition() {
        moveX = null;
        moveY = null;
        ViewFinderConfig.set(ViewFinderConfig.POS_X, -1.0);
        ViewFinderConfig.set(ViewFinderConfig.POS_Y, -1.0);
    }

    // ---------------------------------------------------------------- 시점

    public static void lookAt(ViewFinderGeometry.Face f) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float yaw = player.getYRot();
        float tYaw, tPitch;
        if (f.dx == 0 && f.dz == 0) {
            tYaw = yaw;
            tPitch = f.dy > 0 ? -90f : 90f;
        } else {
            tYaw = (float) Math.toDegrees(Math.atan2(-f.nx, f.nz));
            tPitch = (float) -Math.toDegrees(Math.asin(f.ny));
        }
        startYaw = yaw;
        startPitch = player.getXRot();
        targetYaw = yaw + Mth.wrapDegrees(tYaw - yaw);
        targetPitch = tPitch;
        animMs = ViewFinderConfig.get(ViewFinderConfig.ANIMATION_MS, 300);
        if (animMs <= 0) {
            animating = false;
            setRotation(player, targetYaw, targetPitch);
        } else {
            animStart = System.nanoTime();
            animating = true;
        }
    }

    /** 매 프레임 호출. */
    public static void updateAnimation() {
        if (!animating) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            animating = false;
            return;
        }
        float t = (System.nanoTime() - animStart) / 1_000_000f / animMs;
        if (t >= 1f) {
            animating = false;
            setRotation(player, targetYaw, targetPitch);
            return;
        }
        float e = 1f - (1f - t) * (1f - t) * (1f - t);
        setRotation(player, Mth.lerp(e, startYaw, targetYaw), Mth.lerp(e, startPitch, targetPitch));
    }

    private static void setRotation(LocalPlayer player, float yaw, float pitch) {
        pitch = Mth.clamp(pitch, -90f, 90f);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
    }
}
