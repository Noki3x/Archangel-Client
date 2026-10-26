"""Builds the nametag badge glyph from the angel-wings logo: thicker strokes so it reads at 8 px,
white wings over a soft blue glow, on a 64x40 canvas (the glyph is drawn 8 px tall in game).

Run from the repository root: python3 tools/nametag/badge.py
"""
from PIL import Image, ImageFilter

LOGO = "src/main/resources/assets/archangelclient/textures/gui/logo.png"
OUT = "src/main/resources/assets/archangelclient/textures/font/badge.png"
W, H = 64, 40

logo = Image.open(LOGO).convert("RGBA")
wings = logo.crop(logo.getbbox())
alpha = wings.split()[3].resize((W * 4 - 24, round((W * 4 - 24) * wings.height / wings.width)), Image.LANCZOS)
alpha = alpha.filter(ImageFilter.MaxFilter(5))  # bolden the line art before shrinking
canvas = Image.new("L", (W * 4, H * 4), 0)
canvas.paste(alpha, ((W * 4 - alpha.width) // 2, (H * 4 - alpha.height) // 2))
shape = canvas.resize((W, H), Image.LANCZOS)

glow = shape.filter(ImageFilter.GaussianBlur(1.6)).point(lambda a: min(255, int(a * 1.6)))
out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
out.paste(Image.new("RGBA", (W, H), (96, 165, 250, 255)), (0, 0), glow)
white = Image.new("RGBA", (W, H), (255, 255, 255, 0))
white.putalpha(shape)
out.alpha_composite(white)
out.save(OUT)
print("wrote", OUT)
