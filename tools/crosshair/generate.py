#!/usr/bin/env python3
"""Generates the custom crosshair textures (white shapes, tinted in game) and their outlines.

Each crosshair is drawn at 4x on a 256 px canvas and downsampled to 64x64 so edges are smooth; the
game draws it at about 15 GUI pixels with linear filtering (see the .png.mcmeta files). The wings
crosshair is traced from the menu logo's white line art. Run from the repository root:
    python3 tools/crosshair/generate.py
Requires Pillow and NumPy.
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "archangelclient")
OUT = os.path.join(ROOT, "textures", "crosshair")
LOGO = os.path.join(ROOT, "textures", "gui", "logo.png")

SIZE = 64
SS = 4                      # supersampling
N = SIZE * SS               # canvas size
C = N / 2                   # center
U = N / 15                  # one vanilla crosshair pixel (the vanilla crosshair is 15 px wide)
OUTLINE = int(U * 0.65)     # outline thickness


def canvas():
    img = Image.new("L", (N, N), 0)
    return img, ImageDraw.Draw(img)


def rect(d, x0, y0, x1, y1):
    d.rectangle([C + x0 * U, C + y0 * U, C + x1 * U - 1, C + y1 * U - 1], fill=255)


def cross(d, arm, gap, t):
    """Four arms of length arm (in vanilla px) starting gap px from the center, t px thick."""
    h = t / 2
    rect(d, -h, -gap - arm, h, -gap)
    rect(d, -h, gap, h, gap + arm)
    rect(d, -gap - arm, -h, -gap, h)
    rect(d, gap, -h, gap + arm, h)


def dot(d, r):
    d.ellipse([C - r * U, C - r * U, C + r * U, C + r * U], fill=255)


def ring(d, r, t):
    d.ellipse([C - r * U, C - r * U, C + r * U, C + r * U], outline=255, width=int(t * U))


def line(d, x0, y0, x1, y1, t):
    d.line([(C + x0 * U, C + y0 * U), (C + x1 * U, C + y1 * U)], fill=255, width=int(t * U))
    for x, y in ((x0, y0), (x1, y1)):  # round caps
        r = t * U / 2
        d.ellipse([C + x * U - r, C + y * U - r, C + x * U + r, C + y * U + r], fill=255)


def poly(d, pts, t=None):
    p = [(C + x * U, C + y * U) for x, y in pts]
    if t is None:
        d.polygon(p, fill=255)
    else:
        d.line(p + [p[0]], fill=255, width=int(t * U), joint="curve")


def wings_mask(scale, dy=0.0):
    """The logo's white line art (its blue glow is left out), centered and scaled to `scale` px wide."""
    logo = np.array(Image.open(LOGO).convert("RGBA")).astype(float)
    r, g, b, a = (logo[..., i] for i in range(4))
    white = np.clip((np.minimum(np.minimum(r, g), b) - 150) / 90, 0, 1) * (a / 255)
    m = Image.fromarray((white * 255).astype(np.uint8))
    m = m.crop(m.getbbox())
    w = int(scale * U)
    h = int(m.height * w / m.width)
    m = m.resize((w, h), Image.LANCZOS)
    # thicken a touch so the strokes survive the trip down to crosshair size
    m = m.filter(ImageFilter.MaxFilter(5))
    img = Image.new("L", (N, N), 0)
    img.paste(m, (int(C - w / 2), int(C - h / 2 + dy * U)))
    return img


# ---------------------------------------------------------------- the styles
def s_wings():
    return wings_mask(14.5)


def s_wings_dot():
    img = wings_mask(13, dy=-1.5)
    d = ImageDraw.Draw(img)
    dot(d, 1.0)
    return img


def simple(fn):
    def make():
        img, d = canvas()
        fn(d)
        return img
    return make


STYLES = [
    # key, display name, drawing
    ("wings", "Archangel Wings", s_wings),
    ("wings_dot", "Wings + Dot", s_wings_dot),
    ("classic", "Classic", simple(lambda d: cross(d, 7, 0, 1) or rect(d, -0.5, -0.5, 0.5, 0.5))),
    ("thin", "Thin Cross", simple(lambda d: cross(d, 6, 0, 0.7))),
    ("bold", "Bold Cross", simple(lambda d: cross(d, 6, 0, 2))),
    ("gap", "Gap Cross", simple(lambda d: cross(d, 4, 2, 1))),
    ("gap_dot", "Gap Cross + Dot", simple(lambda d: (cross(d, 4, 2.5, 1), dot(d, 0.75)))),
    ("small", "Small Cross", simple(lambda d: cross(d, 2.5, 0.5, 1))),
    ("dot", "Dot", simple(lambda d: dot(d, 1))),
    ("big_dot", "Big Dot", simple(lambda d: dot(d, 2))),
    ("circle", "Circle", simple(lambda d: ring(d, 5, 1))),
    ("circle_dot", "Circle + Dot", simple(lambda d: (ring(d, 5, 1), dot(d, 0.9)))),
    ("ring_cross", "Ring Cross", simple(lambda d: (ring(d, 4.5, 0.9), cross(d, 2.5, 4.8, 0.9)))),
    ("scope", "Scope", simple(lambda d: (ring(d, 6, 0.8), cross(d, 7, 1.2, 0.8)))),
    ("t_shape", "T", simple(lambda d: (rect(d, -0.5, 2, 0.5, 7), rect(d, -7, -0.5, -2, 0.5), rect(d, 2, -0.5, 7, 0.5)))),
    ("x", "X", simple(lambda d: (line(d, -4.5, -4.5, 4.5, 4.5, 1), line(d, -4.5, 4.5, 4.5, -4.5, 1)))),
    ("x_gap", "X Gap", simple(lambda d: [line(d, sx * 1.6, sy * 1.6, sx * 5, sy * 5, 1) for sx in (-1, 1) for sy in (-1, 1)])),
    ("chevron", "Chevron", simple(lambda d: (line(d, -4, 3, 0, -1, 1.1), line(d, 0, -1, 4, 3, 1.1)))),
    ("caret_dot", "Caret + Dot", simple(lambda d: (line(d, -3.5, 4.5, 0, 1.5, 1), line(d, 0, 1.5, 3.5, 4.5, 1), dot(d, 0.8)))),
    ("brackets", "Brackets", simple(lambda d: (
        rect(d, -6, -4, -5, 4), rect(d, -6, -4, -3.5, -3), rect(d, -6, 3, -3.5, 4),
        rect(d, 5, -4, 6, 4), rect(d, 3.5, -4, 6, -3), rect(d, 3.5, 3, 6, 4), dot(d, 0.7)))),
    ("corners", "Corners", simple(lambda d: [
        (rect(d, sx * 5 - (1 if sx > 0 else 0), sy * 5 - (1 if sy > 0 else 0), sx * 5 + (0 if sx > 0 else 1), sy * 5 + (0 if sy > 0 else 1)),
         rect(d, min(sx * 5, sx * 2.5), sy * 5 - (1 if sy > 0 else 0), max(sx * 5, sx * 2.5), sy * 5 + (0 if sy > 0 else 1)),
         rect(d, sx * 5 - (1 if sx > 0 else 0), min(sy * 5, sy * 2.5), sx * 5 + (0 if sx > 0 else 1), max(sy * 5, sy * 2.5)))
        for sx in (-1, 1) for sy in (-1, 1)])),
    ("square", "Square", simple(lambda d: poly(d, [(-4, -4), (4, -4), (4, 4), (-4, 4)], 1))),
    ("square_dot", "Square + Dot", simple(lambda d: (poly(d, [(-4, -4), (4, -4), (4, 4), (-4, 4)], 0.9), dot(d, 0.8)))),
    ("diamond", "Diamond", simple(lambda d: poly(d, [(0, -5), (5, 0), (0, 5), (-5, 0)], 1))),
    ("diamond_dot", "Diamond + Dot", simple(lambda d: (poly(d, [(0, -5), (5, 0), (0, 5), (-5, 0)], 0.9), dot(d, 0.8)))),
    ("triangle", "Triangle", simple(lambda d: poly(d, [(0, -4.5), (4.5, 3.5), (-4.5, 3.5)], 1))),
    ("star", "Sparkle", simple(lambda d: poly(d, [
        (math.cos(a) * (5.5 if i % 2 == 0 else 1.4), math.sin(a) * (5.5 if i % 2 == 0 else 1.4))
        for i, a in enumerate([k * math.pi / 4 - math.pi / 2 for k in range(8)])]))),
    ("halo", "Halo", simple(lambda d: (ring(d, 3.5, 0.9), cross(d, 2.5, 4.8, 0.9)))),
    ("heart", "Heart", simple(lambda d: poly(d, [
        (16 * math.sin(t) ** 3 / 3.6, -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)) / 3.6 - 0.6)
        for t in [i / 60 * 2 * math.pi for i in range(60)]], 0.9))),
    ("plus", "Plus", simple(lambda d: (rect(d, -1, -4, 1, 4), rect(d, -4, -1, 4, 1)))),
]


def outline_of(mask):
    """A dark rim around the shape (drawn under it) so it stays visible on bright backgrounds."""
    return mask.filter(ImageFilter.MaxFilter(OUTLINE * 2 + 1))


def save(mask, name):
    small = mask.resize((SIZE, SIZE), Image.LANCZOS)
    img = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0))
    img.putalpha(small)
    path = os.path.join(OUT, name + ".png")
    img.save(path, optimize=True)
    with open(path + ".mcmeta", "w") as f:
        json.dump({"texture": {"blur": True, "clamp": True}}, f)
        f.write("\n")


def main():
    os.makedirs(OUT, exist_ok=True)
    for key, _, draw in STYLES:
        mask = draw()
        save(mask, key)
        save(outline_of(mask), key + "_outline")
    print("\n".join('    %s("%s", "%s"),' % (k.upper(), k, n) for k, n, _ in STYLES))


if __name__ == "__main__":
    main()
