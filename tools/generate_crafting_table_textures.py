"""生成「方块染色台」的 5 张 16×16 贴图（原创，不依赖原版素材）。

设计：
  top    浅木台面 + 内嵌 3×3 色卡格（一眼看出是"调色"用的台子）
  front  台沿 + 三个颜料罐 + 双抽屉
  side   竖木纹 + 一条颜料流痕
  back   纯木背板
  bottom 深色底座 + 四颗脚钉
"""

import os

from PIL import Image

OUT = r"D:\MC-Auto\out\ct"
S = 16

# 木色（整体提亮）
W0 = (231, 198, 152)   # 高光
W1 = (207, 167, 111)   # 主色
W2 = (176, 138, 85)    # 暗部
W3 = (139, 106, 66)    # 深缝
W4 = (96, 73, 46)      # 描边
SHADOW = (72, 54, 34)

# 色卡（取自 MARD 基础色）
SW = [
    (232, 69, 47), (240, 155, 58), (245, 217, 59),
    (111, 194, 74), (63, 191, 168), (60, 127, 214),
    (140, 91, 216), (231, 101, 168), (242, 242, 242),
]


def canvas():
    return [[None] * S for _ in range(S)]


def px(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img[y][x] = c


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, c)


def grain(img, x0, y0, x1, y1, c, step=7):
    """稀疏木纹：用哈希散点，避免出现对角线。"""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if img[y][x] is None:
                continue
            h = (x * 73856093) ^ (y * 19349663)
            if h % step == 0:
                px(img, x, y, c)


def border(img, c):
    rect(img, 0, 0, S - 1, 0, c)
    rect(img, 0, S - 1, S - 1, S - 1, c)
    rect(img, 0, 0, 0, S - 1, c)
    rect(img, S - 1, 0, S - 1, S - 1, c)


def base_wood():
    img = canvas()
    rect(img, 0, 0, 15, 15, W1)
    grain(img, 1, 1, 14, 14, W2, 5)
    border(img, W4)
    return img


def top():
    img = canvas()
    rect(img, 0, 0, 15, 15, W0)
    grain(img, 0, 0, 15, 15, W1, 4)
    border(img, W4)
    # 台面内沿
    rect(img, 1, 1, 14, 14, W3)
    rect(img, 2, 2, 13, 13, SHADOW)
    # 3×3 色卡：每格 3×3，间隔 1px
    for gy in range(3):
        for gx in range(3):
            x = 3 + gx * 4
            y = 3 + gy * 4
            c = SW[gy * 3 + gx]
            rect(img, x, y, x + 2, y + 2, c)
            px(img, x, y, tuple(min(255, v + 30) for v in c))
            px(img, x + 2, y + 2, tuple(int(v * 0.75) for v in c))
    return img


def front():
    img = base_wood()
    # 台沿
    rect(img, 1, 1, 14, 2, W0)
    rect(img, 1, 3, 14, 3, W3)
    # 一排色样（5 个 2×3 小色块）
    for i in range(4):
        x = 2 + i * 3
        c = SW[i * 2]
        rect(img, x, 5, x + 1, 7, c)
        px(img, x, 5, tuple(min(255, v + 40) for v in c))
        px(img, x + 1, 7, tuple(int(v * 0.7) for v in c))
    # 搁板
    rect(img, 1, 9, 14, 9, W3)
    px(img, 1, 10, W2)
    px(img, 14, 10, W2)
    # 抽屉面
    rect(img, 2, 11, 13, 14, W0)
    rect(img, 2, 11, 13, 11, W3)
    rect(img, 6, 12, 9, 12, W4)       # 拉手
    px(img, 6, 13, W2)
    px(img, 9, 13, W2)
    return img


def side():
    img = base_wood()
    # 竖木纹
    for x in (5, 10):
        rect(img, x, 1, x, 14, W2)
    # 颜料流痕（上宽下窄，带一点黄）
    rect(img, 7, 1, 8, 4, SW[0])
    rect(img, 7, 5, 8, 6, SW[0])
    px(img, 7, 7, SW[0])
    px(img, 8, 5, SW[2])
    # 钉子
    px(img, 2, 2, W0)
    px(img, 13, 13, W3)
    return img


def back():
    img = base_wood()
    rect(img, 1, 1, 14, 2, W0)
    for x in (5, 10, 14):
        rect(img, x, 3, x, 14, W2)
    # 木节
    px(img, 7, 9, W3)
    px(img, 8, 9, W4)
    px(img, 7, 10, W4)
    return img


def bottom():
    img = canvas()
    rect(img, 0, 0, 15, 15, W2)
    border(img, W4)
    rect(img, 2, 2, 13, 13, W3)
    rect(img, 3, 3, 12, 12, W1)
    grain(img, 3, 3, 12, 12, W2, 11)
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        px(img, x, y, SHADOW)
    return img


def save(img, name):
    out = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            c = img[y][x] or (0, 0, 0, 0)
            out.putpixel((x, y), c + (255,) if len(c) == 3 else c)
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".png")
    out.save(path)
    return path


if __name__ == "__main__":
    for name, fn in (("top", top), ("front", front), ("side", side), ("back", back), ("bottom", bottom)):
        print(save(fn(), name))
