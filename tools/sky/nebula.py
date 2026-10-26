"""Generates the Archangel nebula skybox: textures/environment/nebula.png.

The six cube faces are laid out 3x2 in one atlas (see FACES). Every texel is colored from its
direction on the unit sphere, so the faces meet without seams. Purely procedural and original.

    python3 tools/sky/nebula.py
"""
import os
import numpy as np
from PIL import Image

SIZE = 512                      # pixels per cube face
SEED = 7
OUT = os.path.join(os.path.dirname(__file__), "../../src/main/resources/assets/archangelclient/textures/environment/nebula.png")

# (center, right, down) per face, in atlas order: row 0 = +X, -X, +Y; row 1 = -Y, +Z, -Z.
# Must match NebulaSky.FACES in the client.
FACES = [
    ((1, 0, 0), (0, 0, -1), (0, -1, 0)),
    ((-1, 0, 0), (0, 0, 1), (0, -1, 0)),
    ((0, 1, 0), (1, 0, 0), (0, 0, 1)),
    ((0, -1, 0), (1, 0, 0), (0, 0, -1)),
    ((0, 0, 1), (1, 0, 0), (0, -1, 0)),
    ((0, 0, -1), (-1, 0, 0), (0, -1, 0)),
]

BASE = np.array([0.020, 0.012, 0.045])      # deep space
NEBULA_A = np.array([0.42, 0.16, 0.78])     # violet
NEBULA_B = np.array([0.85, 0.30, 0.75])     # magenta core
NEBULA_C = np.array([0.18, 0.30, 0.85])     # blue fringe


def hash3(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def value_noise(p, seed):
    """Smooth 3D value noise in [0, 1] for points p of shape (..., 3)."""
    i = np.floor(p).astype(np.int64)
    f = p - i
    f = f * f * (3 - 2 * f)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * hash3(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out


def fbm(p, seed, octaves=6):
    total, amp, norm = 0.0, 0.5, 0.0
    for o in range(octaves):
        total = total + amp * value_noise(p, seed + o * 31)
        norm += amp
        p = p * 2.03
        amp *= 0.5
    return total / norm


def face_dirs(face):
    c, r, d = (np.array(v, dtype=np.float64) for v in face)
    t = (np.arange(SIZE) + 0.5) / SIZE * 2 - 1
    u, v = np.meshgrid(t, t)
    dirs = c + u[..., None] * r + v[..., None] * d
    return dirs / np.linalg.norm(dirs, axis=-1, keepdims=True)


def shade(dirs, big_stars, rng_seed):
    p = dirs * 2.2
    warp = np.stack([fbm(p + 11.3, SEED + 1, 4), fbm(p + 47.1, SEED + 2, 4), fbm(p + 83.7, SEED + 3, 4)], axis=-1)
    n = fbm(p + warp * 1.6, SEED, 6)
    # A band of nebula across the sky, thicker on one side.
    band = np.exp(-((dirs[..., 1] * 0.9 + dirs[..., 0] * 0.4) ** 2) * 3.0)
    cloud = np.clip((n - 0.38) * 2.4, 0, 1) ** 1.6 * (0.35 + 0.9 * band)
    core = np.clip((n - 0.58) * 4.0, 0, 1) ** 2 * band
    fringe = fbm(p * 1.7 + 5.0, SEED + 9, 4)

    color = BASE + cloud[..., None] * (NEBULA_A * (1 - fringe[..., None]) + NEBULA_C * fringe[..., None]) + core[..., None] * NEBULA_B

    # Dust: darker filaments over the clouds.
    dust = np.clip((fbm(p * 3.1 + 21.0, SEED + 5, 5) - 0.5) * 3, 0, 1)
    color *= 1 - 0.55 * dust[..., None] * band[..., None]

    # Faint single-texel stars, denser inside the nebula band.
    rng = np.random.default_rng(rng_seed)
    sparkle = rng.random(dirs.shape[:2])
    density = 0.0035 + 0.006 * band
    stars = np.where(sparkle < density, (rng.random(dirs.shape[:2]) ** 3) * 0.9 + 0.1, 0)
    color += stars[..., None] * np.array([0.92, 0.90, 1.0])

    # Bright stars with a small soft glow, placed on the sphere so they cross face edges.
    for sd, size, bright, tint in big_stars:
        cosang = np.clip(dirs @ sd, -1, 1)
        ang = np.arccos(cosang)
        mask = ang < size * 8
        if not mask.any():
            continue
        g = np.exp(-(ang / size) ** 2) * bright + np.exp(-(ang / (size * 3.5)) ** 2) * bright * 0.25
        color += (g * mask)[..., None] * tint
    return np.clip(color, 0, 1)


def main():
    rng = np.random.default_rng(SEED)
    big = []
    for _ in range(180):
        v = rng.normal(size=3)
        v /= np.linalg.norm(v)
        tint = np.array([1.0, 0.85, 1.0]) if rng.random() < 0.5 else np.array([0.75, 0.85, 1.0])
        big.append((v, rng.uniform(0.0016, 0.0042), rng.uniform(0.5, 1.0), tint))
    atlas = np.zeros((SIZE * 2, SIZE * 3, 3))
    for idx, face in enumerate(FACES):
        row, col = divmod(idx, 3)
        atlas[row * SIZE:(row + 1) * SIZE, col * SIZE:(col + 1) * SIZE] = shade(face_dirs(face), big, SEED * 100 + idx)
        print("face", idx, "done")
    img = (atlas ** (1 / 1.1) * 255).astype(np.uint8)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(img, "RGB").convert("RGBA").save(OUT, optimize=True)
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
