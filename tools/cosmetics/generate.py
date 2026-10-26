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


# The elytra uses the same texture as the cape: a 10x20x2 box at (22, 0). Its two big faces are
# at (24, 2) and (36, 2), with thin edges around them.
ELYTRA_FACE_W, ELYTRA_FACE_H = 10 * S, 20 * S
ELYTRA_FACES = ((24 * S, 2 * S), (36 * S, 2 * S))


def paint_elytra(img, glow, face, edge, glow_face=None):
    """Stretches the cape's outer face over both elytra wings, so the elytra matches the cape."""
    d = ImageDraw.Draw(img)
    d.rectangle([24 * S, 0, 44 * S - 1, 2 * S - 1], fill=edge)        # top and bottom
    d.rectangle([22 * S, 2 * S, 24 * S - 1, 22 * S - 1], fill=edge)   # inner edge
    d.rectangle([34 * S, 2 * S, 36 * S - 1, 22 * S - 1], fill=edge)   # outer edge
    wing = face.resize((ELYTRA_FACE_W, ELYTRA_FACE_H), Image.LANCZOS)
    img.paste(wing, ELYTRA_FACES[0])
    img.paste(wing.transpose(Image.FLIP_LEFT_RIGHT), ELYTRA_FACES[1])
    if glow_face is not None:
        g = glow_face.resize((ELYTRA_FACE_W, ELYTRA_FACE_H), Image.LANCZOS)
        glow.paste(g, ELYTRA_FACES[0])
        glow.paste(g.transpose(Image.FLIP_LEFT_RIGHT), ELYTRA_FACES[1])


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
    paint_elytra(img, glow, face, edge, glow_face)
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


def make_sakura_cape():
    """Pale pink sky with a dark branch and drifting cherry blossoms."""
    rng = random.Random(23)
    fw, fh = FACE[2], FACE[3]
    a = vertical_gradient(fw, fh, hex_rgb("#FFE4EF"), hex_rgb("#F9A8D4"))
    border(a, 1, hex_rgb("#F472B6"), hex_rgb("#DB2777"))
    face = Image.fromarray(a)
    glow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    fd, gd = ImageDraw.Draw(face), ImageDraw.Draw(glow)
    # a branch reaching in from the top left
    branch = [(0, 10), (10, 14), (18, 13), (26, 20), (33, 22)]
    fd.line(branch, fill=hex_rgb("#4A2A35") + (255,), width=2)
    fd.line([(10, 14), (14, 24), (13, 30)], fill=hex_rgb("#4A2A35") + (255,), width=1)
    fd.line([(26, 20), (30, 30)], fill=hex_rgb("#4A2A35") + (255,), width=1)

    def blossom(x, y, r, color):
        for k in range(5):
            ang = k / 5 * 2 * math.pi - math.pi / 2
            px, py = x + math.cos(ang) * r, y + math.sin(ang) * r
            fd.ellipse([px - r * 0.7, py - r * 0.7, px + r * 0.7, py + r * 0.7], fill=color + (255,))
        fd.point((x, y), fill=hex_rgb("#FDE68A") + (255,))
        gd.ellipse([x - r, y - r, x + r, y + r], fill=hex_rgb("#FBCFE8") + (150,))

    for x, y in ((9, 12), (17, 11), (25, 18), (14, 25), (31, 23), (29, 29)):
        blossom(x, y, 1.6, hex_rgb("#FFFFFF"))
    for _ in range(14):  # falling petals
        x, y = rng.randrange(3, fw - 3), rng.randrange(28, fh - 3)
        c = lerp(hex_rgb("#FFFFFF"), hex_rgb("#F472B6"), rng.random() * 0.6)
        fd.ellipse([x - 1, y, x + 1, y + 1], fill=c + (255,))
        gd.point((x, y), fill=c + (120,))
    inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#2A1720"), hex_rgb("#3B1D2C")))
    img, glow_full = cape_canvas(face, inner, hex_rgb("#EC4899") + (255,), glow)
    save(img, "cape", "sakura.png")
    save(glow_full, "cape", "sakura_glow.png")


def palette_map(f, palette):
    """Colors a 0..1 field with a list of RGB stops."""
    h, w = f.shape
    a = np.zeros((h, w, 4), dtype=np.uint8)
    idx = np.clip(f, 0, 1) * (len(palette) - 1)
    lo = np.clip(idx.astype(int), 0, len(palette) - 2)
    t = idx - lo
    for c in range(3):
        pa = np.array([p[c] for p in palette], dtype=float)
        a[..., c] = (pa[lo] + (pa[lo + 1] - pa[lo]) * t).astype(np.uint8)
    a[..., 3] = 255
    return a


def make_inferno_cape(frames=6):
    """Animated: flames rising from the hem."""
    fw, fh = FACE[2], FACE[3]
    palette = [hex_rgb("#12070A"), hex_rgb("#5B0F0F"), hex_rgb("#C2410C"), hex_rgb("#F97316"), hex_rgb("#FDE68A")]
    yy, xx = np.mgrid[0:fh, 0:fw].astype(float)
    u, v = xx / fw, yy / fh
    for i in range(frames):
        phase = i / frames * 6.283
        flick = (np.sin(u * 17 + phase + np.sin(v * 9 - phase) * 1.4) * 0.5
                 + np.sin(u * 31 - phase * 2 + v * 5) * 0.3
                 + np.sin(v * 23 + phase * 3) * 0.2)
        f = np.clip(v * 1.25 - 0.35 + flick * 0.28, 0, 1) ** 1.2
        a = palette_map(f, palette)
        border(a, 1, hex_rgb("#F97316"), hex_rgb("#FACC15"))
        glow = np.zeros_like(a)
        glow[..., :3] = a[..., :3]
        glow[..., 3] = (np.clip((f - 0.55) / 0.45, 0, 1) * 230).astype(np.uint8)
        glow[0, :] = glow[-1, :] = (251, 146, 60, 220)
        glow[:, 0] = glow[:, -1] = (250, 204, 21, 220)
        inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#140A0A"), hex_rgb("#2A0E0A")))
        img, glow_full = cape_canvas(Image.fromarray(a), inner, hex_rgb("#9A3412") + (255,), Image.fromarray(glow))
        save(img, "cape", "inferno_%d.png" % i)
        save(glow_full, "cape", "inferno_%d_glow.png" % i)


def make_aurora_cape(frames=8):
    """Animated: green and violet curtains of light over a starry night."""
    rng = random.Random(31)
    fw, fh = FACE[2], FACE[3]
    st = stars(rng, fw - 4, fh - 4, 22)
    yy, xx = np.mgrid[0:fh, 0:fw].astype(float)
    u, v = xx / fw, yy / fh
    for i in range(frames):
        phase = i / frames * 6.283
        base = vertical_gradient(fw, fh, hex_rgb("#050816"), hex_rgb("#0B1730")).astype(float)
        light = np.zeros((fh, fw, 3))
        for k, (col, off, spd) in enumerate([("#34D399", 0.0, 1.0), ("#22D3EE", 1.7, -1.3), ("#A78BFA", 3.1, 0.8)]):
            center = 0.5 + 0.28 * np.sin(v * 5 + phase * spd + off) + 0.08 * np.sin(v * 13 - phase + k)
            band = np.exp(-((u - center) / 0.09) ** 2) * np.clip(1.2 - v, 0, 1) * (0.6 + 0.4 * np.sin(v * 30 + phase * 2 + k))
            light += band[..., None] * np.array(hex_rgb(col), dtype=float)
        a = base.copy()
        a[..., :3] = np.clip(base[..., :3] + light * 0.85, 0, 255)
        a = a.astype(np.uint8)
        intensity = np.clip(light.max(axis=2) / 255, 0, 1)
        border(a, 1, hex_rgb("#34D399"), hex_rgb("#A78BFA"))
        glow = np.zeros_like(a)
        glow[..., :3] = a[..., :3]
        glow[..., 3] = (np.clip((intensity - 0.3) / 0.7, 0, 1) * 210).astype(np.uint8)
        for x, y, b in st:
            tw = 0.5 + 0.5 * math.sin(phase * 2 + b * 17)
            c = lerp(hex_rgb("#C7D2FE"), (255, 255, 255), tw)
            a[y + 2, x + 2, :3] = c
            glow[y + 2, x + 2] = c + (int(200 * tw),)
        glow[0, :] = glow[-1, :] = (52, 211, 153, 210)
        glow[:, 0] = glow[:, -1] = (167, 139, 250, 210)
        inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#070B18"), hex_rgb("#0C1426")))
        img, glow_full = cape_canvas(Image.fromarray(a), inner, hex_rgb("#1F5F5B") + (255,), Image.fromarray(glow))
        save(img, "cape", "aurora_%d.png" % i)
        save(glow_full, "cape", "aurora_%d_glow.png" % i)


def make_frost_cape():
    """Icy blue with a glowing six-armed snowflake."""
    rng = random.Random(41)
    fw, fh = FACE[2], FACE[3]
    a = vertical_gradient(fw, fh, hex_rgb("#E0F2FE"), hex_rgb("#38BDF8"))
    border(a, 1, hex_rgb("#FFFFFF"), hex_rgb("#7DD3FC"))
    face = Image.fromarray(a)
    glow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    # draw the snowflake at 4x, then scale down for smooth arms
    big = Image.new("L", (fw * 4, fh * 4), 0)
    bd = ImageDraw.Draw(big)
    cx, cy, r = fw * 2, fh * 1.6, fw * 1.5
    for k in range(6):
        ang = k / 6 * 2 * math.pi
        ex, ey = cx + math.cos(ang) * r, cy + math.sin(ang) * r
        bd.line([(cx, cy), (ex, ey)], fill=255, width=5)
        for t in (0.45, 0.72):  # little side branches
            bx, by = cx + math.cos(ang) * r * t, cy + math.sin(ang) * r * t
            for side in (-1, 1):
                ba = ang + side * math.radians(40)
                bd.line([(bx, by), (bx + math.cos(ba) * r * 0.25, by + math.sin(ba) * r * 0.25)], fill=255, width=4)
    bd.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], fill=255)
    flake = big.resize((fw, fh), Image.LANCZOS)
    tint = Image.new("RGBA", (fw, fh), hex_rgb("#FFFFFF") + (0,))
    tint.putalpha(flake)
    face.alpha_composite(tint)
    gt = Image.new("RGBA", (fw, fh), hex_rgb("#BAE6FD") + (0,))
    gt.putalpha(flake)
    glow.alpha_composite(gt)
    gd = ImageDraw.Draw(glow)
    fd = ImageDraw.Draw(face)
    for _ in range(18):  # frost sparkles
        x, y = rng.randrange(2, fw - 2), rng.randrange(2, fh - 2)
        fd.point((x, y), fill=(255, 255, 255, 255))
        gd.point((x, y), fill=(255, 255, 255, 180))
    inner = Image.fromarray(vertical_gradient(fw, fh, hex_rgb("#0B1622"), hex_rgb("#0E2233")))
    img, glow_full = cape_canvas(face, inner, hex_rgb("#7DD3FC") + (255,), glow)
    save(img, "cape", "frost.png")
    save(glow_full, "cape", "frost_glow.png")


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


class BoxModel:
    """Packs box-UV cubes into a texture as they're added, painting each one a flat color."""

    def __init__(self, tw, th):
        self.arr = np.zeros((th, tw, 4), dtype=np.uint8)
        self.cubes = []
        self.u = self.v = self.row_h = 0

    def add(self, origin, size, side, top, shade=0.2, **extra):
        w, h, d = size
        rw, rh = int(math.ceil(2 * (w + d))), int(math.ceil(h + d))
        if self.u + rw > self.arr.shape[1]:
            self.u, self.v, self.row_h = 0, self.v + self.row_h, 0
        paint_box(self.arr, self.u, self.v, size, side, top, shade)
        self.cubes.append(dict({"origin": origin, "size": size, "uv": [self.u, self.v]}, **extra))
        self.u += rw
        self.row_h = max(self.row_h, rh)

    def save(self, name):
        save(Image.fromarray(self.arr), "hat", name + ".png")
        h, w = self.arr.shape[:2]
        return geometry(name, w, h, [{"name": name, "pivot": [0, 24, 0], "cubes": self.cubes}])


def make_wizard_hat():
    """A tall violet cone, leaning back a little, with a gold band."""
    m = BoxModel(64, 64)
    violet, violet_top, gold = hex_rgb("#4C1D95"), hex_rgb("#6D28D9"), hex_rgb("#F5C46B")
    m.add([-6, 32, -6], [12, 1, 12], violet, violet_top, 0)
    m.add([-4.5, 33, -4.5], [9, 1, 9], gold, hex_rgb("#FFE3A3"), 0)
    m.add([-4, 34, -4], [8, 3, 8], violet, violet_top)
    m.add([-3, 37, -2.5], [6, 3, 6], violet, violet_top)
    m.add([-2, 40, -1.5], [4, 3, 4], violet, violet_top)
    m.add([-1, 43, -0.5], [2, 2, 2], violet, violet_top)
    m.add([-0.5, 45, 0.5], [1, 1, 1], gold, hex_rgb("#FFE3A3"), 0)
    return m.save("wizard_hat")


def make_horns():
    """Two small curved horns, dark red fading to black at the tips."""
    m = BoxModel(32, 32)
    base, mid, tip = hex_rgb("#7F1D1D"), hex_rgb("#450A0A"), hex_rgb("#1C0606")
    for side in (-1, 1):
        x = 2 if side > 0 else -4
        m.add([x, 32, -1], [2, 2, 2], base, hex_rgb("#991B1B"))
        m.add([x + side * 0.5, 34, -0.75], [1.5, 2, 1.5], mid, mid)
        m.add([x + side * 1.25 + (0.25 if side < 0 else 0), 36, -0.5], [1, 1.5, 1], tip, tip, 0)
    return m.save("horns")


def make_cat_ears():
    """Soft charcoal cat ears with pink insides."""
    m = BoxModel(32, 32)
    fur, fur_top, pink = hex_rgb("#2B2D35"), hex_rgb("#3A3D48"), hex_rgb("#F9A8D4")
    for side in (-1, 1):
        x = 1 if side > 0 else -4
        m.add([x, 32, -0.5], [3, 1, 1], fur, fur_top, 0)
        m.add([x + 0.5, 33, -0.5], [2, 1, 1], fur, fur_top, 0)
        m.add([x + (1.5 if side > 0 else 0.5), 34, -0.5], [1, 1, 1], fur, fur_top, 0)
        m.add([x + 0.75, 32.25, -0.6], [1.5, 1.25, 0.1], pink, pink, 0)
    return m.save("cat_ears")


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
    # added in 0.2.0
    make_sakura_cape()
    make_inferno_cape()
    make_aurora_cape()
    make_frost_cape()
    make_wings("phoenix", hex_rgb("#FFB347"), hex_rgb("#E2502C"), hex_rgb("#FFD27A"), hex_rgb("#FF8A3D"),
               hex_rgb("#FFD27A"), hex_rgb("#F97316"))
    make_wings("frost", hex_rgb("#F0FDFF"), hex_rgb("#67E8F9"), hex_rgb("#FFFFFF"), hex_rgb("#A5F3FC"),
               hex_rgb("#FFFFFF"), hex_rgb("#CFFAFE"))
    make_wings("shadow", hex_rgb("#1A1A20"), hex_rgb("#2A0E14"), hex_rgb("#EF4444"), hex_rgb("#DC2626"),
               hex_rgb("#111114"), hex_rgb("#1F0A0E"))
    make_wings("sakura", hex_rgb("#FFF1F7"), hex_rgb("#F9A8D4"), hex_rgb("#EC4899"), hex_rgb("#F9A8D4"),
               hex_rgb("#FFFFFF"), hex_rgb("#FBCFE8"))
    make_halo("golden", hex_rgb("#FFF3C4"), hex_rgb("#F5C46B"), hex_rgb("#FCD34D"))
    make_halo("inferno", hex_rgb("#FDBA74"), hex_rgb("#EF4444"), hex_rgb("#FB923C"))
    save_model("wizard_hat", make_wizard_hat())
    save_model("horns", make_horns())
    save_model("cat_ears", make_cat_ears())


if __name__ == "__main__":
    main()
