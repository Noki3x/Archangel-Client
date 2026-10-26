"""Draws the emote wheel icons: white line-art figures on transparency, 64x64.

Run from the repository root: python3 tools/emotes/icons.py
Each figure is a head and limbs given as polylines on a 16x16 grid (y down), drawn at 8x and
downsampled for smooth edges. All original artwork.
"""
import os
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/archangelclient/textures/emote"
S = 64
K = 8  # supersampling
W = 1.25  # stroke width in grid units

# grid points: neck at (8, 5.5), hips at (8, 10)
FIGURES = {
    "wave": {"head": (8, 3.4), "lines": [[(8, 5.5), (8, 10)], [(8, 10), (6.4, 14)], [(8, 10), (9.6, 14)],
                                          [(8, 6.2), (6.2, 9.6)], [(8, 6.2), (11.4, 4.6), (12.6, 1.8)]],
             "extra": [("arc", (11.0, 1.4, 14.4, 4.8), 290, 20), ("arc", (11.6, 0.6, 15.6, 4.6), 290, 20)]},
    "dab": {"head": (7.0, 4.0), "lines": [[(8, 5.5), (8, 10)], [(8, 10), (6.4, 14)], [(8, 10), (9.6, 14)],
                                         [(8, 6.2), (11.2, 4.2), (14.2, 1.8)], [(8, 6.2), (5.0, 5.0), (8.8, 3.0)]]},
    "clap": {"head": (8, 3.4), "lines": [[(8, 5.5), (8, 10)], [(8, 10), (6.4, 14)], [(8, 10), (9.6, 14)],
                                          [(8, 6.2), (5.6, 8.4), (7.6, 7.0)], [(8, 6.2), (10.4, 8.4), (8.4, 7.0)]],
             "extra": [("line", [(5.0, 5.4), (4.0, 4.6)]), ("line", [(11.0, 5.4), (12.0, 4.6)]), ("line", [(8, 4.6 + 0.0), (8, 4.6)])]},
    "floss": {"head": (8, 3.4), "lines": [[(8, 5.5), (7.4, 10)], [(7.4, 10), (6.0, 14)], [(7.4, 10), (9.0, 14)],
                                           [(8, 6.2), (11.2, 8.6)], [(8, 6.2), (11.8, 7.2)]],
              "extra": [("line", [(3.2, 8.0), (4.6, 8.0)]), ("line", [(2.6, 9.6), (4.2, 9.6)])]},
    "sit": {"head": (6.4, 4.6), "lines": [[(6.4, 6.7), (6.4, 11.2)], [(6.4, 11.2), (10.6, 11.2), (11.2, 14.2)],
                                         [(6.4, 7.4), (9.4, 9.8)]],
            "extra": [("line", [(3.0, 14.2), (13.6, 14.2)])]},
    "bow": {"head": (11.6, 6.6), "lines": [[(9.8, 6.8), (6.6, 9.6)], [(6.6, 9.6), (6.0, 14)], [(6.6, 9.6), (7.6, 14)],
                                          [(9.4, 7.2), (9.8, 10.6)], [(9.0, 7.4), (7.2, 8.6)]]},
    "salute": {"head": (8, 3.4), "lines": [[(8, 5.5), (8, 10)], [(8, 10), (6.4, 14)], [(8, 10), (9.6, 14)],
                                            [(8, 6.2), (6.2, 9.6)], [(8, 6.2), (11.4, 4.4), (9.6, 2.0)]]},
    "cheer": {"head": (8, 4.4), "lines": [[(8, 6.5), (8, 10.6)], [(8, 10.6), (6.4, 14.2)], [(8, 10.6), (9.6, 14.2)],
                                           [(8, 7.0), (5.4, 4.6), (4.8, 1.6)], [(8, 7.0), (10.6, 4.6), (11.2, 1.6)]],
              "extra": [("line", [(2.4, 2.8), (3.4, 3.4)]), ("line", [(13.6, 2.8), (12.6, 3.4)])]},
}


def px(p):
    u = S * K / 16
    return (p[0] * u, p[1] * u)


def draw(fig):
    img = Image.new("L", (S * K, S * K), 0)
    d = ImageDraw.Draw(img)
    w = int(W * S * K / 16)
    r = 1.75 * S * K / 16

    def poly(points, width=w):
        pts = [px(p) for p in points]
        d.line(pts, fill=255, width=width, joint="curve")
        for p in pts:  # round caps
            d.ellipse((p[0] - width / 2, p[1] - width / 2, p[0] + width / 2, p[1] + width / 2), fill=255)

    for line in fig["lines"]:
        poly(line)
    hx, hy = px(fig["head"])
    d.ellipse((hx - r, hy - r, hx + r, hy + r), fill=255)
    for kind, *args in fig.get("extra", []):
        if kind == "line":
            poly(args[0], int(w * 0.7))
        elif kind == "arc":
            (x0, y0, x1, y1), a0, a1 = args
            p0, p1 = px((x0, y0)), px((x1, y1))
            d.arc((p0[0], p0[1], p1[0], p1[1]), a0, a1, fill=255, width=int(w * 0.7))
    alpha = img.resize((S, S), Image.LANCZOS)
    out = Image.new("RGBA", (S, S), (255, 255, 255, 0))
    out.putalpha(alpha)
    return out


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fig in FIGURES.items():
        draw(fig).save(os.path.join(OUT, f"{name}.png"))
        with open(os.path.join(OUT, f"{name}.png.mcmeta"), "w") as f:
            f.write('{"texture": {"blur": true}}\n')
    sheet = Image.new("RGBA", (S * len(FIGURES), S), (30, 31, 36, 255))
    for i, name in enumerate(FIGURES):
        icon = Image.open(os.path.join(OUT, f"{name}.png"))
        sheet.alpha_composite(icon, (i * S, 0))
    sheet.save("/tmp/emote_icons_sheet.png")
    print(f"wrote {len(FIGURES)} icons to {OUT}")
