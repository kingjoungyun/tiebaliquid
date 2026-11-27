import math
from PIL import Image, ImageDraw, ImageFont

RES = "app/src/main/res"
SS = 4  # supersample


def droplet_mask(size):
    s = size * SS
    m = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(m)
    cx, cy, r = s / 2, s * 0.625, s * 0.3125
    apex_y = cy - r * 1.4
    ang = math.radians(45)
    p1 = (cx + r * math.cos(ang), cy - r * math.sin(ang))
    p2 = (cx - r * math.cos(ang), cy - r * math.sin(ang))
    d.polygon([(cx, apex_y), p1, p2], fill=255)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def gradient(size, top, bottom):
    g = Image.new("RGBA", (1, size))
    for y in range(size):
        t = y / max(1, size - 1)
        g.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,))
    return g.resize((size, size))


def droplet(size):
    mask = droplet_mask(size)
    body = gradient(size, (61, 155, 255), (10, 132, 255))
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    im.paste(body, (0, 0), mask)
    # 极淡的高光，避免看成“洞”
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(hl)
    r = size * 0.3125
    cx, cy = size / 2, size * 0.625
    hw, hh = r * 0.20, r * 0.30
    hx, hy = cx - r * 0.36, cy - r * 0.26
    d.ellipse([hx - hw, hy - hh, hx + hw, hy + hh], fill=(255, 255, 255, 70))
    hl.putalpha(Image.composite(hl.getchannel("A"), Image.new("L", (size, size), 0), mask))
    return Image.alpha_composite(im, hl)


# 1) splash 图标
droplet(480).save(f"{RES}/drawable-xxxhdpi/ic_splash.png")

# 2) splash 品牌文字
FONT = f"{RES}/font/harmony_sans_sc_semibold.ttf"
W, H = 532, 213
im = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
text = "Tiebaliquid"
size = 118
while size > 20:
    font = ImageFont.truetype(FONT, size * 2)
    box = d.textbbox((0, 0), text, font=font)
    tw, th = box[2] - box[0], box[3] - box[1]
    if tw <= W * 2 * 0.86 and th <= H * 2 * 0.70:
        break
    size -= 2
d.text(((W * 2 - tw) / 2 - box[0], (H * 2 - th) / 2 - box[1]), text, font=font, fill=(10, 132, 255, 255))
im.resize((W, H), Image.LANCZOS).save(f"{RES}/drawable-xxxhdpi/ic_splash_text.png")
print("done")
