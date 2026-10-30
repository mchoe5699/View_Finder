package com.qwerty.viewfinder.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qwerty.viewfinder.ViewFinderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 뷰 파인더 투영·그리기·클릭 판정.
 * 화면 가운데에 보이는 면 = 지금 바라보는 방향, 오른쪽 면 = 내 오른쪽 방향, 위쪽 면 = 머리 위 방향.
 */
public final class ViewFinderRenderer {
    private static final int COLOR_FACE = 0xEEF2F7;
    private static final int COLOR_EDGE = 0xCDD6E1;
    private static final int COLOR_CORNER = 0xB4C0CE;
    private static final int COLOR_HOVER = 0x4DA3FF;
    private static final int COLOR_OUTLINE = 0x2B3440;
    private static final int COLOR_LABEL = 0x1E2630;

    public record Layout(int x, int y, int size) {
        public float centerX() { return x + size / 2f; }
        public float centerY() { return y + size / 2f; }
        public float scale() { return size * 0.5f / ViewFinderGeometry.RADIUS * 0.97f; }
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    public static final class Proj {
        public final ViewFinderGeometry.Face face;
        public final float[] xs, ys;
        public final float facing;
        public final float cx, cy;

        Proj(ViewFinderGeometry.Face face, float[] xs, float[] ys, float facing, float cx, float cy) {
            this.face = face;
            this.xs = xs;
            this.ys = ys;
            this.facing = facing;
            this.cx = cx;
            this.cy = cy;
        }
    }

    private ViewFinderRenderer() {}

    public static List<Proj> project(float yawDeg, float pitchDeg, Layout l) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        float sy = (float) Math.sin(y), cy = (float) Math.cos(y);
        float sp = (float) Math.sin(p), cp = (float) Math.cos(p);
        // 앞(시선), 오른쪽, 위 방향 (마인크래프트 좌표)
        float fx = -sy * cp, fy = -sp, fz = cy * cp;
        float rx = -cy, rz = -sy;
        float ux = -sy * sp, uy = cp, uz = cy * sp;

        float ox = l.centerX(), oy = l.centerY(), s = l.scale();
        List<Proj> out = new ArrayList<>();
        for (ViewFinderGeometry.Face f : ViewFinderGeometry.FACES) {
            float facing = fx * f.nx + fy * f.ny + fz * f.nz;
            if (facing <= 1e-3f) continue;
            int n = f.verts.length;
            float[] xs = new float[n], ys = new float[n];
            for (int i = 0; i < n; i++) {
                float[] v = f.verts[i];
                xs[i] = ox + (rx * v[0] + rz * v[2]) * s;
                ys[i] = oy - (ux * v[0] + uy * v[1] + uz * v[2]) * s;
            }
            float pcx = ox + (rx * f.cx + rz * f.cz) * s;
            float pcy = oy - (ux * f.cx + uy * f.cy + uz * f.cz) * s;
            out.add(new Proj(f, xs, ys, facing, pcx, pcy));
        }
        return out;
    }

    public static ViewFinderGeometry.Face hit(List<Proj> faces, double mx, double my) {
        for (Proj p : faces) {
            if (inside(p.xs, p.ys, mx, my)) return p.face;
        }
        return null;
    }

    private static boolean inside(float[] xs, float[] ys, double x, double y) {
        boolean in = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
            if ((ys[i] > y) != (ys[j] > y)
                    && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
                in = !in;
            }
        }
        return in;
    }

    public static void draw(GuiGraphics g, Layout l, float yaw, float pitch,
                            ViewFinderGeometry.Face hover, boolean interacting) {
        List<Proj> faces = project(yaw, pitch, l);
        float alpha = ViewFinderConfig.get(ViewFinderConfig.OPACITY, 0.9).floatValue();

        if (interacting) {
            g.fill(l.x() - 3, l.y() - 3, l.x() + l.size() + 3, l.y() + l.size() + 3, 0x40000000);
        }

        Matrix4f mat = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (Proj p : faces) {
            int base;
            if (p.face == hover) base = COLOR_HOVER;
            else if (p.face.kind == ViewFinderGeometry.Kind.FACE) base = COLOR_FACE;
            else if (p.face.kind == ViewFinderGeometry.Kind.EDGE) base = COLOR_EDGE;
            else base = COLOR_CORNER;
            float shade = 0.62f + 0.38f * p.facing;
            float r = ((base >> 16) & 0xFF) / 255f * shade;
            float gr = ((base >> 8) & 0xFF) / 255f * shade;
            float b = (base & 0xFF) / 255f * shade;
            for (int i = 1; i < p.xs.length - 1; i++) {
                bb.vertex(mat, p.xs[0], p.ys[0], 0).color(r, gr, b, alpha).endVertex();
                bb.vertex(mat, p.xs[i], p.ys[i], 0).color(r, gr, b, alpha).endVertex();
                bb.vertex(mat, p.xs[i + 1], p.ys[i + 1], 0).color(r, gr, b, alpha).endVertex();
            }
        }

        float lw = Math.max(0.5f, l.size() / 90f);
        float or = ((COLOR_OUTLINE >> 16) & 0xFF) / 255f;
        float og = ((COLOR_OUTLINE >> 8) & 0xFF) / 255f;
        float ob = (COLOR_OUTLINE & 0xFF) / 255f;
        float oa = Math.min(1f, alpha + 0.05f);
        for (Proj p : faces) {
            int n = p.xs.length;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                line(bb, mat, p.xs[i], p.ys[i], p.xs[j], p.ys[j], lw, or, og, ob, oa);
            }
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        Font font = Minecraft.getInstance().font;

        if (ViewFinderConfig.get(ViewFinderConfig.SHOW_LABELS, true)) {
            float base = Math.max(0.45f, l.size() / 72f);
            for (Proj p : faces) {
                if (p.face.kind != ViewFinderGeometry.Kind.FACE || p.facing < 0.35f) continue;
                Component c = ViewFinderText.direction(p.face);
                if (c == null) continue;
                float vis = Math.min(1f, (p.facing - 0.35f) / 0.3f);
                int a = (int) (255 * vis * alpha);
                if (a < 16) continue;
                int color = (a << 24) | (p.face == hover ? 0xFFFFFF : COLOR_LABEL);
                float ts = base * (0.55f + 0.45f * p.facing);
                g.pose().pushPose();
                g.pose().translate(p.cx, p.cy, 10);
                g.pose().scale(ts, ts, 1f);
                g.drawString(font, c, -font.width(c) / 2, -4, color, false);
                g.pose().popPose();
            }
        }

        if (ViewFinderConfig.get(ViewFinderConfig.SHOW_HEADING, true) || hover != null) {
            Component line = hover != null ? ViewFinderText.direction(hover) : ViewFinderText.heading(yaw, pitch);
            if (line != null) {
                float ts = Math.max(0.6f, Math.min(1f, l.size() / 80f));
                g.pose().pushPose();
                g.pose().translate(l.centerX(), l.y() + l.size() + 2, 10);
                g.pose().scale(ts, ts, 1f);
                int w = font.width(line);
                g.drawString(font, line, -w / 2, 0, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }
    }

    private static void line(BufferBuilder bb, Matrix4f mat, float x1, float y1, float x2, float y2, float w,
                             float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float px = -dy / len * w * 0.5f, py = dx / len * w * 0.5f;
        bb.vertex(mat, x1 + px, y1 + py, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x1 - px, y1 - py, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x2 - px, y2 - py, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x1 + px, y1 + py, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x2 - px, y2 - py, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x2 + px, y2 + py, 0).color(r, g, b, a).endVertex();
    }
}
