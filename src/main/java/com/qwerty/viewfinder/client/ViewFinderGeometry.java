package com.qwerty.viewfinder.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 모서리와 꼭짓점을 45도로 깎은 육면체.
 * 6개 면(정사각형) + 12개 모서리 면(직사각형) + 8개 꼭짓점 면(삼각형) = 26개 클릭 영역.
 * 좌표계는 마인크래프트 월드와 같다: +X 동, +Y 위, +Z 남.
 */
public final class ViewFinderGeometry {
    public enum Kind { FACE, EDGE, CORNER }

    /** 깎는 양 (정육면체 반변 길이 1 기준). */
    public static final float CHAMFER = 0.34f;

    public static final class Face {
        public final Kind kind;
        public final int dx, dy, dz;
        public final float nx, ny, nz;
        public final float[][] verts;
        public final float cx, cy, cz;

        Face(Kind kind, int dx, int dy, int dz, float[][] verts) {
            this.kind = kind;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            this.nx = dx / len;
            this.ny = dy / len;
            this.nz = dz / len;
            this.verts = verts;
            float sx = 0, sy = 0, sz = 0;
            for (float[] v : verts) {
                sx += v[0];
                sy += v[1];
                sz += v[2];
            }
            this.cx = sx / verts.length;
            this.cy = sy / verts.length;
            this.cz = sz / verts.length;
        }
    }

    public static final List<Face> FACES;
    /** 원점에서 가장 먼 꼭짓점까지 거리. */
    public static final float RADIUS;

    static {
        List<Face> list = new ArrayList<>();
        float m = 1f - CHAMFER;
        int[] signs = {1, -1};

        // 6 면
        for (int a = 0; a < 3; a++) {
            int b = (a + 1) % 3, c = (a + 2) % 3;
            for (int s : signs) {
                float[][] uv = {{m, m}, {-m, m}, {-m, -m}, {m, -m}};
                float[][] verts = new float[4][];
                for (int i = 0; i < 4; i++) {
                    float[] v = new float[3];
                    v[a] = s;
                    v[b] = uv[i][0];
                    v[c] = uv[i][1];
                    verts[i] = v;
                }
                int[] d = new int[3];
                d[a] = s;
                list.add(new Face(Kind.FACE, d[0], d[1], d[2], verts));
            }
        }

        // 12 모서리
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 3; b++) {
                int t = 3 - a - b;
                for (int sa : signs) {
                    for (int sb : signs) {
                        float[][] verts = new float[4][3];
                        verts[0][a] = sa;     verts[0][b] = sb * m; verts[0][t] = m;
                        verts[1][a] = sa * m; verts[1][b] = sb;     verts[1][t] = m;
                        verts[2][a] = sa * m; verts[2][b] = sb;     verts[2][t] = -m;
                        verts[3][a] = sa;     verts[3][b] = sb * m; verts[3][t] = -m;
                        int[] d = new int[3];
                        d[a] = sa;
                        d[b] = sb;
                        list.add(new Face(Kind.EDGE, d[0], d[1], d[2], verts));
                    }
                }
            }
        }

        // 8 꼭짓점
        for (int sx : signs) {
            for (int sy : signs) {
                for (int sz : signs) {
                    float[][] verts = {
                            {sx, sy * m, sz * m},
                            {sx * m, sy, sz * m},
                            {sx * m, sy * m, sz}
                    };
                    list.add(new Face(Kind.CORNER, sx, sy, sz, verts));
                }
            }
        }

        FACES = Collections.unmodifiableList(list);
        RADIUS = (float) Math.sqrt(1 + 2 * m * m);
    }

    private ViewFinderGeometry() {}
}
