#!/usr/bin/env python3
"""Generates Archangel's original cosmetic textures and Blockbench geometry.

Everything here is drawn procedurally, so the assets are our own (no Mojang textures are used or
derived). Run from the repository root:  python3 tools/cosmetics/generate.py
Requires Pillow and NumPy. Output goes to src/main/resources/assets/archangelclient/.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "archangelclient")
TEX = os.path.join(ROOT, "textures", "cosmetic")
MODELS = os.path.join(ROOT, "cosmetics", "models")


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full, optimize=True)


def save_model(name, geometry):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".geo.json"), "w") as f:
        json.dump(geometry, f, indent=2)
        f.write("\n")


def geometry(identifier, tw, th, bones):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.archangel." + identifier,
                "texture_width": tw,
                "texture_height": th,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }


# ---------------------------------------------------------------- capes
# Vanilla cape layout (64x32 units), drawn at 4x: outer face at (1,1)-(11,17), inner face at
# (12,1)-(22,17), edges around them.
S = 4
CAPE_W, CAPE_H = 64 * S, 32 * S
FACE = (1 * S, 1 * S, 10 * S, 16 * S)       # x, y, w, h of the outer face
INNER = (12 * S, 1 * S, 10 * S, 16 * S)


def cape_canvas(face, inner, edge, glow_face=None):
    img = Image.new("RGBA", (CAPE_W, CAPE_H), (0, 0, 0, 0))
    glow = Image.new("RGBA", (CAPE_W, CAPE_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # edges and top/bottom strips
    d.rectangle([0, 1 * S, 1 * S - 1, 17 * S - 1], fill=edge)
    d.rectangle([11 * S, 1 * S, 12 * S - 1, 17 * S - 1], fill=edge)
    d.rectangle([1 * S, 0, 11 * S - 1, 1 * S - 1], fill=edge)
    d.rectangle([11 * S, 0, 21 * S - 1, 1 * S - 1], fill=edge)
    img.paste(face, FACE[:2])
    img.paste(inner, INNER[:2])
    if glow_face is not None:
        glow.paste(glow_face, FACE[:2])
    return img, glow


def vertical_gradient(w, h, top, bottom):
    a = np.zeros((h, w, 4), dtype=np.uint8)
    for y in range(h):
        c = lerp(top, bottom, y / max(1, h - 1))
        a[y, :, :3] = c
        a[y, :, 3] = 255
    return a


def border(arr, width, c1, c2, alpha=255):
    h, w = arr.shape[:2]
    for y in range(h):
        c = lerp(c1, c2, y / (h - 1))
        for x in range(w):
            if x < width or x >= w - width or y < width or y >= h - width:
                arr[y, x, :3] = c
                arr[y, x, 3] = alpha
    return arr


def wing_emblem(size, color):
    """Two stylised wings meeting at a small diamond, as a mask image."""
    w, h = size
    m = Image.new("L", (w * 4, h * 4), 0)
    d = ImageDraw.Draw(m)
    cx, cy = w * 2, h * 2
    for side in (-1, 1):
        for i in range(4):
            # feathers fan out and down from the center
            length = (w * 1.9) * (1 - i * 0.17)
            ang = math.radians(18 + i * 16)
            x0, y0 = cx + side * w * 0.18, cy - h * 0.5 + i * h * 0.32
            x1 = x0 + side * length * math.cos(ang)
            y1 = y0 + length * math.sin(ang) * 0.55 - h * 0.6
            thick = h * 0.42 * (1 - i * 0.12)
            d.polygon([(x0, y0 - thick * 0.5), (x1, y1), (x0, y0 + thick)], fill=255)
    d.polygon([(cx, cy - h * 1.0), (cx + w * 0.22, cy + h * 0.1), (cx, cy + h * 1.2), (cx - w * 0.22, cy + h * 0.1)], fill=255)
    m = m.resize((w, h), Image.LANCZOS)
    img = Image.new("RGBA", (w, h), color + (0,))
    img.putalpha(m)
    return img


def make_archangel_cape():
    fw, fh = FACE[2], FACE[3]
    a = vertical_gradient(fw, fh, hex_rgb("#1E3A8A"), hex_rgb("#3B1A6E"))
    # soft diagonal light from the top right
    yy, xx = np.mgrid[0:fh, 0:fw]
    ray = np.clip(1 - np.abs((xx - fw) * 0.8 + yy * 0.6 - fw * 0.4) / (fw * 0.5), 0, 1) * 40
    a[..., :3] = np.clip(a[..., :3].astype(int) + ray[..., None].astype(int), 0, 255)
    border(a, 2, hex_rgb("#60A5FA"), hex_rgb("#8B5CF6"))
    face = Image.fromarray(a)
    emblem = wing_emblem((fw - 8, 22), hex_rgb("#DBEAFE"))
    face.alpha_composite(emblem, (4, 14))
    # little star under the emblem
    d = ImageDraw.Draw(face)
    sx, sy = fw // 2, 46
    d.polygon([(sx, sy - 5), (sx + 2, sy), (sx, sy + 5), (sx - 2, sy)], fill=hex_rgb("#93C5FD") + (255,))

    glow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    g = np.array(glow)
    border(g, 2, hex_rgb("#93C5FD"), hex_rgb("#A78BFA"), 230)
    g[2:-2, 2:-2] = 0
    glow = Image.fromarray(g)
    glow.alpha_composite(wing_emblem((fw - 8, 22), hex_rgb("#BFDBFE")), (4, 14))
    ImageDraw.Draw(glow).polygon([(sx, sy - 5), (sx + 2, sy), (sx, sy + 5), (sx - 2, sy)], fill=hex_rgb("#BFDBFE") + (255,))

    inner = Image.fromarray(border(vertical_gradient(fw, fh, hex_rgb("#14161C"), hex_rgb("#1A1530")), 1,
                                   hex_rgb("#2A3B66"), hex_rgb("#3A2A66")))
    img, glow_full = cape_canvas(face, inner, hex_rgb("#4F7BE0") + (255,), glow)
    save(img, "cape", "archangel.png")
    save(glow_full, "cape", "archangel_glow.png")


def stars(rng, w, h, count):
    return [(rng.randrange(w), rng.randrange(h), rng.random()) for _ in range(count)]


def make_midnight_cape():
    rng = random.Random(7)
    fw, fh = FACE[2], FACE[3]
    a = vertical_gradient(fw, fh, hex_rgb("#070B1A"), hex_rgb("#141A3D"))
    border(a, 1, hex_rgb("#6D5BD0"), hex_rgb("#3B82F6"))
    face = Image.fromarray(a)
    glow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    fd, gd = ImageDraw.Draw(face), ImageDraw.Draw(glow)
    for x, y, b in stars(rng, fw - 4, fh - 4, 38):
        x += 2
        y += 2
        c = lerp(hex_rgb("#93C5FD"), (255, 255, 255), b)
        if b > 0.8:
            for px, py in ((x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                fd.point((px, py), fill=c + (255,))
                gd.point((px, py), fill=c + (255,))
        else:
            fd.point((x, y), fill=c + (255,))
            gd.point((x, y), fill=c + (int(120 + 135 * b),))
    # crescent moon
    moon = Image.new("L", (64, 64), 0)
    md = ImageDraw.Draw(moon)
    md.ellipse([8, 8, 56, 56], fill=255)
    md.ellipse([22, 2, 66, 50], fill=0)
    moon = moon.resize((14, 14), Image.LANCZOS)
    mc = Image.new("RGBA", (14, 14), hex_rgb("#E0E7FF") + (0,))
    mc.putalpha(moon)
    face.alpha_composite(mc, (fw // 2 - 7, 10))
    glow.alpha_composite(mc, (fw // 2 - 7, 10))
    g = np.array(glow)
    g[0, :] = g[-1, :] = (167, 139, 250, 200)
    g[:, 0] = g[:, -1] = (167, 139, 250, 200)
    inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#0B0E18"), hex_rgb("#10142A")))
    img, glow_full = cape_canvas(face, inner, hex_rgb("#2B2F6B") + (255,), Image.fromarray(g))
    save(img, "cape", "midnight.png")
    save(glow_full, "cape", "midnight_glow.png")


def nebula_field(w, h, phase):
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    u, v = xx / w, yy / h
    f = np.zeros((h, w))
    for k, (fx, fy, sp, amp) in enumerate([(2.0, 1.2, 1.0, 1.0), (3.5, 2.7, -1.6, 0.6), (6.0, 4.1, 2.3, 0.35), (1.1, 3.3, 0.7, 0.5)]):
        f += amp * np.sin(fx * u * 6.283 + fy * v * 6.283 * 0.7 + phase * sp + k + np.sin(v * 7 + phase * 0.5 * sp) * 0.9)
    f = (f - f.min()) / (f.max() - f.min())
    return f


def make_nebula_cape(frames=8):
    rng = random.Random(11)
    fw, fh = FACE[2], FACE[3]
    st = stars(rng, fw - 4, fh - 4, 30)
    palette = [hex_rgb("#070A1F"), hex_rgb("#1E2A78"), hex_rgb("#3B82F6"), hex_rgb("#8B5CF6"), hex_rgb("#F0ABFC")]
    for i in range(frames):
        phase = i / frames * 6.283
        f = nebula_field(fw, fh, phase)
        a = np.zeros((fh, fw, 4), dtype=np.uint8)
        idx = f * (len(palette) - 1)
        lo = np.clip(idx.astype(int), 0, len(palette) - 2)
        t = idx - lo
        for c in range(3):
            pa = np.array([p[c] for p in palette])
            a[..., c] = (pa[lo] + (pa[lo + 1] - pa[lo]) * t).astype(np.uint8)
        a[..., 3] = 255
        border(a, 1, hex_rgb("#60A5FA"), hex_rgb("#C084FC"))
        glow = np.zeros_like(a)
        bright = np.clip((f - 0.62) / 0.38, 0, 1)
        glow[..., :3] = a[..., :3]
        glow[..., 3] = (bright * 200).astype(np.uint8)
        for x, y, b in st:
            tw = 0.5 + 0.5 * math.sin(phase * 2 + b * 20)
            c = lerp(hex_rgb("#BFDBFE"), (255, 255, 255), tw)
            a[y + 2, x + 2, :3] = c
            glow[y + 2, x + 2] = c + (int(255 * tw),)
        glow[0, :] = glow[-1, :] = (147, 197, 253, 220)
        glow[:, 0] = glow[:, -1] = (192, 132, 252, 220)
        inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#0A0C1C"), hex_rgb("#160F2E")))
        img, glow_full = cape_canvas(Image.fromarray(a), inner, hex_rgb("#3A3FA0") + (255,), Image.fromarray(glow))
        save(img, "cape", "nebula_%d.png" % i)
        save(glow_full, "cape", "nebula_%d_glow.png" % i)


# ---------------------------------------------------------------- wings
WS = 4  # wing textures are 4x the 64x64 model units


def leaf(cx, cy, ang, length, width, steps=18):
    """Rounded feather outline starting at (cx, cy) and pointing along ang."""
    pts = []
    dx, dy = math.cos(ang), math.sin(ang)
    px, py = -dy, dx
    for i in range(steps + 1):
        t = i / steps
        w = width * math.sin(math.pi * min(1.0, t * 1.15)) ** 0.7 * (1 - 0.25 * t)
        pts.append((cx + dx * length * t + px * w, cy + dy * length * t + py * w))
    for i in range(steps, -1, -1):
        t = i / steps
        w = width * math.sin(math.pi * min(1.0, t * 1.15)) ** 0.7 * (1 - 0.25 * t)
        pts.append((cx + dx * length * t - px * w, cy + dy * length * t - py * w))
    return pts


def wing_mask(w, h, coverts=False):
    """Feathered wing silhouette. x=0 is the wing tip, x=w the shoulder; y=0 is the top."""
    sw, sh = w * 4, h * 4
    m = Image.new("L", (sw, sh), 0)
    d = ImageDraw.Draw(m)
    lines = Image.new("L", (sw, sh), 0)
    ld = ImageDraw.Draw(lines)
    n = 9 if not coverts else 6
    for i in range(n):
        s = i / (n - 1)                       # 0 at the shoulder, 1 at the tip
        # roots follow the arched bone of the wing
        root_x = sw * (0.92 - s * 0.78)
        root_y = sh * (0.2 - 0.14 * math.sin(s * math.pi * 0.8) + 0.05 * s)
        if not coverts:
            length = sh * (0.62 + 0.2 * math.sin(s * math.pi * 0.9))
            ang = math.radians(96 + s * 34)
            width = sw * 0.075
        else:
            length = sh * (0.55 + 0.15 * math.sin(s * math.pi))
            ang = math.radians(98 + s * 30)
            width = sw * 0.1
        pts = leaf(root_x, root_y, ang, length, width)
        d.polygon(pts, fill=255)
        ld.line([(root_x, root_y), (root_x + math.cos(ang) * length * 0.92, root_y + math.sin(ang) * length * 0.92)],
                fill=255, width=max(2, int(sw * 0.01)))
    # the wing's arm: a smooth arch from the shoulder out to the tip
    arch = []
    for i in range(25):
        t = i / 24
        arch.append((sw * (1 - t * 0.9), sh * (0.24 - 0.2 * math.sin(t * math.pi * 0.85) + 0.04 * t)))
    for i in range(24, -1, -1):
        t = i / 24
        arch.append((sw * (1 - t * 0.9), sh * (0.24 - 0.2 * math.sin(t * math.pi * 0.85) + 0.04 * t) + sh * (0.24 - 0.12 * t)))
    d.polygon(arch, fill=255)
    m = m.filter(ImageFilter.GaussianBlur(1.0)).point(lambda v: 255 if v > 110 else 0)
    return m.resize((w, h), Image.LANCZOS), lines.resize((w, h), Image.LANCZOS)


def wing_layer(w, h, top, bottom, edge, glow_color, coverts=False):
    mask, lines = wing_mask(w, h, coverts)
    base = Image.fromarray(vertical_gradient(w, h, top, bottom))
    m = np.array(mask).astype(float) / 255
    ln = np.array(lines).astype(float) / 255
    a = np.array(base).astype(float)
    # darker feather shafts, brighter rim
    a[..., :3] = a[..., :3] * (1 - 0.18 * ln[..., None])
    edge_mask = np.array(mask.filter(ImageFilter.FIND_EDGES)).astype(float) / 255
    edge_mask = np.clip(np.array(Image.fromarray((edge_mask * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3))) / 255, 0, 1)
    ec = np.array(edge, dtype=float)
    a[..., :3] = a[..., :3] * (1 - edge_mask[..., None] * 0.8) + ec * edge_mask[..., None] * 0.8
    a[..., 3] = m * 255
    tex = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    g = np.zeros((h, w, 4), dtype=np.uint8)
    g[..., :3] = glow_color
    yy = np.mgrid[0:h, 0:w][0] / h
    g[..., 3] = np.clip((edge_mask * 0.9 + ln * 0.25 + yy * 0.35) * m * 255, 0, 255).astype(np.uint8)
    return tex, Image.fromarray(g)


def mirrored_pair(img):
    w, h = img.size
    out = Image.new("RGBA", (w * 2, h), (0, 0, 0, 0))
    out.paste(img, (0, 0))
    out.paste(img.transpose(Image.FLIP_LEFT_RIGHT), (w, 0))
    return out


def make_wings(name, top, bottom, edge, glow_color, cov_top, cov_bottom):
    img = Image.new("RGBA", (64 * WS, 64 * WS), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64 * WS, 64 * WS), (0, 0, 0, 0))
    main, main_g = wing_layer(20 * WS, 24 * WS, top, bottom, edge, glow_color)
    cov, cov_g = wing_layer(14 * WS, 14 * WS, cov_top, cov_bottom, edge, glow_color, coverts=True)
    img.paste(mirrored_pair(main), (0, 0))
    glow.paste(mirrored_pair(main_g), (0, 0))
    img.paste(mirrored_pair(cov), (0, 32 * WS))
    glow.paste(mirrored_pair(cov_g), (0, 32 * WS))
    save(img, "wings", name + ".png")
    save(glow, "wings", name + "_glow.png")


def wings_model():
    def wing(side):
        x0 = 1 if side > 0 else -21
        x1 = 1 if side > 0 else -15
        return {
            "name": "left_wing" if side > 0 else "right_wing",
            "parent": "wings",
            "pivot": [side * 1.5, 22, 2.5],
            "rotation": [0, side * -28, 0],
            "cubes": [
                {"origin": [x0, 7, 2.5], "size": [20, 24, 0], "uv": [0, 0], "mirror": side < 0},
                {"origin": [x1 if side < 0 else 1, 17, 2.2], "size": [14, 14, 0], "uv": [0, 32], "mirror": side < 0},
            ],
        }
    return geometry("seraph_wings", 64, 64, [{"name": "wings", "pivot": [0, 24, 0]}, wing(1), wing(-1)])


# ---------------------------------------------------------------- halos
def halo_model(radius=4.6, segments=14, height=35.5):
    cubes = []
    seg_len = 2 * radius * math.sin(math.pi / segments) + 0.45
    for i in range(segments):
        a = i / segments * 2 * math.pi
        cx, cz = radius * math.cos(a), radius * math.sin(a)
        cubes.append({
            "origin": [round(cx - seg_len / 2, 4), height, round(cz - 0.5, 4)],
            "size": [round(seg_len, 4), 1, 1],
            "pivot": [round(cx, 4), height + 0.5, round(cz, 4)],
            "rotation": [0, round(-math.degrees(a) + 90, 3), 0],
            "uv": [0, 0],
        })
    return geometry("halo", 32, 32, [{"name": "halo", "pivot": [0, 24, 0], "cubes": cubes}])


def make_halo(name, c1, c2, glow):
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    a = np.zeros((16, 64, 4), dtype=np.uint8)
    for x in range(64):
        c = lerp(c1, c2, abs(math.sin(x / 64 * math.pi)))
        a[:, x, :3] = c
    a[..., 3] = 255
    img.paste(Image.fromarray(a), (0, 0))
    g = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    ga = a.copy()
    ga[..., :3] = glow
    ga[..., 3] = 230
    g.paste(Image.fromarray(ga), (0, 0))
    save(img, "halo", name + ".png")
    save(g, "halo", name + "_glow.png")


# ---------------------------------------------------------------- hats
def box_region(u, v, w, h, d):
    """Pixel rectangles (x, y, w, h) of a box-UV cube's faces: top, bottom, then the four sides."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
    }


def paint_box(arr, u, v, size, side, top, shade=0.0):
    w, h, d = size
    for face, (x, y, fw, fh) in box_region(u, v, w, h, d).items():
        c = top if face == "top" else side
        if face == "bottom":
            c = lerp(side, (0, 0, 0), 0.4)
        for yy in range(int(math.ceil(fh))):
            for xx in range(int(math.ceil(fw))):
                k = lerp(c, (0, 0, 0), shade * yy / max(1, fh)) if face not in ("top", "bottom") else c
                px, py = int(x + xx), int(y + yy)
                if 0 <= px < arr.shape[1] and 0 <= py < arr.shape[0]:
                    arr[py, px, :3] = k
                    arr[py, px, 3] = 255


def make_crown():
    gold, gold_top, gem = hex_rgb("#F5C46B"), hex_rgb("#FFE3A3"), hex_rgb("#3B82F6")
    arr = np.zeros((32, 64, 4), dtype=np.uint8)
    cubes, u, v, row_h = [], 0, 0, 0

    def add(origin, size, side, top, shade=0.25):
        nonlocal u, v, row_h
        w, h, d = size
        rw, rh = int(math.ceil(2 * (w + d))), int(math.ceil(h + d))
        if u + rw > 64:
            u, v, row_h = 0, v + row_h, 0
        paint_box(arr, u, v, size, side, top, shade)
        cubes.append({"origin": origin, "size": size, "uv": [u, v]})
        u += rw
        row_h = max(row_h, rh)

    # band, slightly outside the head (which spans -4..4)
    add([-4.5, 32, -4.5], [9, 2, 1], gold, gold_top)
    add([-4.5, 32, 3.5], [9, 2, 1], gold, gold_top)
    add([3.5, 32, -3.5], [1, 2, 7], gold, gold_top)
    add([-4.5, 32, -3.5], [1, 2, 7], gold, gold_top)
    for x in (-4.5, -0.5, 3.5):
        tall = 3 if x == -0.5 else 2
        add([x, 34, -4.5], [1, tall, 1], gold, gold_top)
        add([x, 34, 3.5], [1, tall, 1], gold, gold_top)
    add([3.5, 34, -0.5], [1, 2, 1], gold, gold_top)
    add([-4.5, 34, -0.5], [1, 2, 1], gold, gold_top)
    add([-1, 32.5, -4.75], [2, 1, 0.25], gem, hex_rgb("#93C5FD"), 0)
    img = Image.fromarray(arr)
    save(img, "hat", "crown.png")
    return geometry("crown", 64, 32, [{"name": "crown", "pivot": [0, 24, 0], "cubes": cubes}])


def make_top_hat():
    arr = np.zeros((64, 64, 4), dtype=np.uint8)
    black, top, band = hex_rgb("#17181D"), hex_rgb("#25272F"), hex_rgb("#3B82F6")
    paint_box(arr, 0, 0, (12, 1, 12), black, top, 0)
    paint_box(arr, 0, 16, (8, 8, 8), black, top, 0.15)
    paint_box(arr, 0, 40, (8, 1, 8), band, hex_rgb("#60A5FA"), 0)
    save(Image.fromarray(arr), "hat", "top_hat.png")
    return geometry("top_hat", 64, 64, [{"name": "top_hat", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-6, 32, -6], "size": [12, 1, 12], "uv": [0, 0]},
        {"origin": [-4, 33, -4], "size": [8, 8, 8], "uv": [0, 16]},
        {"origin": [-4, 33.5, -4], "size": [8, 1, 8], "uv": [0, 40], "inflate": 0.25},
    ]}])


# ---------------------------------------------------------------- aura
def make_aura_swirl():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(float) / n * 2 * math.pi
    f = (np.sin(xx * 2 + np.sin(yy * 3) * 1.2) + np.sin(yy * 4 + xx) * 0.7 + np.sin((xx - yy) * 3) * 0.5)
    f = (f - f.min()) / (f.max() - f.min())
    f = 0.25 + 0.75 * f ** 1.6
    a = np.zeros((n, n, 4), dtype=np.uint8)
    a[..., 0] = a[..., 1] = a[..., 2] = (f * 255).astype(np.uint8)
    a[..., 3] = 255
    save(Image.fromarray(a), "aura", "swirl.png")


def main():
    make_archangel_cape()
    make_midnight_cape()
    make_nebula_cape()
    make_wings("seraph", hex_rgb("#FFFFFF"), hex_rgb("#A9C8FF"), hex_rgb("#60A5FA"), hex_rgb("#93C5FD"),
               hex_rgb("#FFFFFF"), hex_rgb("#DCE8FF"))
    make_wings("void", hex_rgb("#2A1650"), hex_rgb("#7C3AED"), hex_rgb("#C084FC"), hex_rgb("#A855F7"),
               hex_rgb("#1A1033"), hex_rgb("#4C1D95"))
    save_model("seraph_wings", wings_model())
    save_model("halo", halo_model())
    make_halo("seraph", hex_rgb("#FFFFFF"), hex_rgb("#93C5FD"), hex_rgb("#BFDBFE"))
    make_halo("nebula", hex_rgb("#C4B5FD"), hex_rgb("#3B82F6"), hex_rgb("#A78BFA"))
    save_model("crown", make_crown())
    save_model("top_hat", make_top_hat())
    make_aura_swirl()


if __name__ == "__main__":
    main()
