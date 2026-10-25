"""Generates the Archangel Client angel-wings logo (white line art) as SVG, then rasterizes PNGs.
The blue glow is added afterwards with ImageMagick because cairosvg ignores SVG blur filters.
Usage: python3 gen_logo.py <out_dir>"""
import math, os, subprocess, sys
import cairosvg

ROOT_TOP = (268, 250)
TIP = (492, 92)
LEAD_C1, LEAD_C2 = (330, 170), (420, 100)       # leading-edge cubic control points
TRAIL = [TIP, (462, 168), (426, 222), (388, 264), (348, 296), (306, 318), (270, 322)]

def cubic(t):
    p0, p1, p2, p3 = ROOT_TOP, LEAD_C1, LEAD_C2, TIP
    u = 1 - t
    return tuple(u**3*a + 3*u*u*t*b + 3*u*t*t*c + t**3*d for a, b, c, d in zip(p0, p1, p2, p3))

def scallop(points, bulge):
    """Open path through points, each segment bulging outward (down/left) to form a feather tip."""
    d = f"M{points[0][0]:.1f},{points[0][1]:.1f}"
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        dx, dy = x1 - x0, y1 - y0
        L = math.hypot(dx, dy)
        nx, ny = dy / L, -dx / L                      # right-hand normal of a tip->root walk points outward
        cx, cy = (x0 + x1) / 2 + nx * bulge, (y0 + y1) / 2 + ny * bulge
        d += f" Q{cx:.1f},{cy:.1f} {x1:.1f},{y1:.1f}"
    return d

def tier(f):
    """Inner feather row: trailing points pulled toward the matching leading-edge point by factor f."""
    n = len(TRAIL)
    pts = []
    for i, (x, y) in enumerate(TRAIL[1:-1], start=1):
        lx, ly = cubic(1 - i / (n - 1))
        pts.append((lx + (x - lx) * f, ly + (y - ly) * f))
    return pts

def wing_paths(detail=True):
    lead = f"M{ROOT_TOP[0]},{ROOT_TOP[1]} C{LEAD_C1[0]},{LEAD_C1[1]} {LEAD_C2[0]},{LEAD_C2[1]} {TIP[0]},{TIP[1]}"
    paths = [lead, scallop(TRAIL, 16), f"M{ROOT_TOP[0]},{ROOT_TOP[1]} L{TRAIL[-1][0]},{TRAIL[-1][1]}"]
    if detail:                                         # inner feather rows; dropped for tiny icons
        paths += [scallop(tier(0.62)[:-1], 11), scallop(tier(0.30)[:-2], 8)]
    return paths

def svg(stroke, detail=True):
    right = "".join(f'<path d="{p}"/>' for p in wing_paths(detail))
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">'
            f'<g transform="translate(0,46)" fill="none" stroke="#FFFFFF" stroke-width="{stroke}" '
            f'stroke-linecap="round" stroke-linejoin="round">{right}'
            f'<g transform="translate(512,0) scale(-1,1)">{right}</g></g></svg>')

def raster(stroke, size, path, detail=True):
    cairosvg.svg2png(bytestring=svg(stroke, detail).encode(), write_to=path, output_width=size, output_height=size)

out = sys.argv[1]; os.makedirs(out, exist_ok=True)
open(os.path.join(out, "logo.svg"), "w").write(svg(9))
# high-res logo: white line art over a soft blue (#3B82F6) glow
lines = os.path.join(out, "_lines.png"); raster(9, 1024, lines)
subprocess.run(["convert", lines,
                "(", "+clone", "-fill", "#3B82F6", "-colorize", "100", "-blur", "0x14", ")",
                "(", "+clone", "-blur", "0x5", ")", "-swap", "0,2", "-background", "none", "-flatten",
                os.path.join(out, "logo.png")], check=True)
os.remove(lines)
# small icons: heavier strokes and no glow so they stay legible
for s, w in ((16, 26), (32, 20), (64, 14), (128, 11)):
    raster(w, s, os.path.join(out, f"icon_{s}.png"), detail=s > 32)
