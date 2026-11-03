"""
뷰 파인더(BE) 텍스처 생성기.
- HUD 프레임: 방위(yaw) 10° x 높이(pitch) 10° 간격으로 미리 그린 깎은 육면체 (36 x 19 = 684장)
- 폼 아이콘: 26방향 (면 6 + 모서리 12 + 꼭짓점 8), 해당 영역을 파랗게 강조
- 아이템 아이콘
좌표계는 마인크래프트와 같다: +X 동, +Y 위, +Z 남. yaw 0 = 남쪽을 봄, pitch 양수 = 아래를 봄.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

OUT = sys.argv[1] if len(sys.argv) > 1 else "resource_pack"
FONT_PATH = "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
FONT_INDEX = 1  # Korean face in the CJK collection

CHAMFER = 0.34
M = 1 - CHAMFER
RADIUS = math.sqrt(1 + 2 * M * M)

COLOR_FACE = (0xEE, 0xF2, 0xF7)
COLOR_EDGE = (0xCD, 0xD6, 0xE1)
COLOR_CORNER = (0xB4, 0xC0, 0xCE)
COLOR_HL = (0x4D, 0xA3, 0xFF)
COLOR_OUTLINE = (0x2B, 0x34, 0x40)
COLOR_LABEL = (0x1E, 0x26, 0x30)

LABELS = {(0, 0, -1): "북", (1, 0, 0): "동", (0, 0, 1): "남", (-1, 0, 0): "서", (0, 1, 0): "위", (0, -1, 0): "아래"}


def build_faces():
    faces = []
    for a in range(3):
        b, c = (a + 1) % 3, (a + 2) % 3
        for s in (1, -1):
            verts = []
            for u, w in ((M, M), (-M, M), (-M, -M), (M, -M)):
                v = [0, 0, 0]
                v[a], v[b], v[c] = s, u, w
                verts.append(v)
            d = [0, 0, 0]
            d[a] = s
            faces.append(("face", tuple(d), verts))
    for a in range(3):
        for b in range(a + 1, 3):
            t = 3 - a - b
            for sa in (1, -1):
                for sb in (1, -1):
                    vs = [[0] * 3 for _ in range(4)]
                    vs[0][a], vs[0][b], vs[0][t] = sa, sb * M, M
                    vs[1][a], vs[1][b], vs[1][t] = sa * M, sb, M
                    vs[2][a], vs[2][b], vs[2][t] = sa * M, sb, -M
                    vs[3][a], vs[3][b], vs[3][t] = sa, sb * M, -M
                    d = [0, 0, 0]
                    d[a], d[b] = sa, sb
                    faces.append(("edge", tuple(d), vs))
    for sx in (1, -1):
        for sy in (1, -1):
            for sz in (1, -1):
                vs = [[sx, sy * M, sz * M], [sx * M, sy, sz * M], [sx * M, sy * M, sz]]
                faces.append(("corner", (sx, sy, sz), vs))
    return faces


FACES = build_faces()
assert len(FACES) == 26


def basis(yaw, pitch):
    y, p = math.radians(yaw), math.radians(pitch)
    sy, cy, sp, cp = math.sin(y), math.cos(y), math.sin(p), math.cos(p)
    f = (-sy * cp, -sp, cy * cp)
    r = (-cy, 0.0, -sy)
    u = (-sy * sp, cp, cy * sp)
    return f, r, u


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def norm(d):
    l = math.sqrt(dot(d, d))
    return (d[0] / l, d[1] / l, d[2] / l)


_font_cache = {}


def font(px):
    px = max(6, int(px))
    if px not in _font_cache:
        _font_cache[px] = ImageFont.truetype(FONT_PATH, px, index=FONT_INDEX)
    return _font_cache[px]


def render(yaw, pitch, size, highlight=None, labels=True, ss=4):
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    f, r, u = basis(yaw, pitch)
    scale = S * 0.5 / RADIUS * 0.96
    ox = oy = S / 2
    vis = []
    for kind, dvec, verts in FACES:
        n = norm(dvec)
        facing = dot(f, n)
        if facing <= 1e-3:
            continue
        pts = [(ox + dot(r, v) * scale, oy - dot(u, v) * scale) for v in verts]
        vis.append((kind, dvec, pts, facing))
    lw = max(1, int(S / 90))
    for kind, dvec, pts, facing in vis:
        base = COLOR_HL if dvec == highlight else {"face": COLOR_FACE, "edge": COLOR_EDGE, "corner": COLOR_CORNER}[kind]
        shade = 0.62 + 0.38 * facing
        col = tuple(int(c * shade) for c in base) + (235,)
        d.polygon(pts, fill=col)
    for kind, dvec, pts, facing in vis:
        d.line(pts + [pts[0]], fill=COLOR_OUTLINE + (245,), width=lw, joint="curve")
    if labels:
        for kind, dvec, pts, facing in vis:
            if kind != "face" or facing < 0.35:
                continue
            a = min(1.0, (facing - 0.35) / 0.3)
            text = LABELS[dvec]
            fs = S * 0.16 * (0.55 + 0.45 * facing) * (0.8 if len(text) > 1 else 1.0)
            cx = sum(p[0] for p in pts) / 4
            cy = sum(p[1] for p in pts) / 4
            colr = (255, 255, 255) if dvec == highlight else COLOR_LABEL
            d.text((cx, cy), text, font=font(fs), fill=colr + (int(255 * a),), anchor="mm")
    return img.resize((size, size), Image.LANCZOS)


def save_small(img, path):
    # 팔레트 PNG로 저장해 팩 용량을 줄인다 (알파 유지).
    img.quantize(colors=160, method=Image.Quantize.FASTOCTREE).save(path, optimize=True)


def look_angles(dvec):
    nx, ny, nz = norm(dvec)
    if nx == 0 and nz == 0:
        return 0.0, (-90.0 if ny > 0 else 90.0)
    yaw = math.degrees(math.atan2(-nx, nz))
    pitch = -math.degrees(math.asin(ny))
    return yaw, pitch


def main():
    tex = os.path.join(OUT, "textures", "ui", "view_finder")
    os.makedirs(tex, exist_ok=True)
    # HUD frames
    n = 0
    for yi in range(36):
        for pi in range(19):
            yaw = yi * 10 - 180
            pitch = pi * 10 - 90
            save_small(render(yaw, pitch, 128), os.path.join(tex, f"f{yi}_{pi}.png"))
            n += 1
    # 26 direction icons: seen from a little off the direction so it reads as 3D
    for kind, dvec, _ in FACES:
        yaw, pitch = look_angles(dvec)
        if abs(pitch) > 80:
            yaw, pitch = 180 + 25, pitch * 0.62
        else:
            yaw, pitch = yaw + 25, max(-80, min(80, pitch + 20))
        name = "icon_" + "_".join({1: "p", -1: "n", 0: "z"}[c] for c in dvec)
        render(yaw, pitch, 128, highlight=dvec).save(os.path.join(tex, name + ".png"), optimize=True)
    # item icon
    items = os.path.join(OUT, "textures", "items")
    os.makedirs(items, exist_ok=True)
    render(180 + 35, 28, 32, labels=False, ss=8).save(os.path.join(items, "view_finder.png"))
    # pack icon
    render(180 + 35, 28, 256, highlight=(0, 0, -1)).save(os.path.join(OUT, "pack_icon.png"))
    print(f"frames={n} icons=26")


if __name__ == "__main__":
    main()
