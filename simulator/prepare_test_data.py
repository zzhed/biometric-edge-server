"""
准备测试数据 — 生成合成人脸，同时输出两种形式：
  1. 合成 jpg (供 preprocess 检测用)
  2. 预对齐 112×112 .npy (直接跳过检测，供 extract→protect→match 链路测试)

每个人不同随机种子 → 从同一个伪 embedding 分布生成模拟"身份特征"的 112×112 图像。
同一个人两张图用不同纹理噪声，但面部几何一致 → embedding 相近 → genuine 高分。
不同人几何差异大 → embedding 差异大 → impostor 低分。
"""

import os
import sys
import numpy as np
import cv2

OUTPUT_DIR  = os.path.dirname(os.path.abspath(__file__))
SAMPLES_DIR = os.path.join(OUTPUT_DIR, "data", "samples")
ALIGNED_DIR = os.path.join(OUTPUT_DIR, "data", "aligned")

NUM_PERSONS = 5
IMAGES_PER_PERSON = 2
CANVAS = 320  # 大画布留足够空间, Haar 在小图上检测效果好


def draw_face_bgr(canvas_size: int, person_seed: int, variant: int) -> np.ndarray:
    """生成一张 BGR 人脸图像（高对比度, OpenCV Haar 可检测）。"""
    seed = person_seed * 1000 + variant
    rng = np.random.RandomState(seed)

    # 亮灰背景
    bg = rng.randint(210, 250)
    img = np.full((canvas_size, canvas_size, 3), bg, dtype=np.uint8)

    cx, cy = canvas_size // 2, canvas_size // 2

    # 肤色椭圆脸 — 更暗更饱和以提高对比度
    face_w = 55 + rng.randint(0, 15)
    face_h = 70 + rng.randint(0, 15)
    skin = (rng.randint(40, 100), rng.randint(60, 120), rng.randint(80, 160))
    cv2.ellipse(img, (cx, cy), (face_w, face_h), 0, 0, 360, skin, -1)

    # 深色头发区域 (脸顶上半圆)
    hair_h = rng.randint(15, 30)
    hair_color = (rng.randint(5, 30), rng.randint(5, 30), rng.randint(5, 30))
    cv2.ellipse(img, (cx, cy - face_h // 2 + hair_h // 2),
                (face_w + 5, hair_h + 5), 0, 0, 180, hair_color, -1)

    # 左眼 — 深色椭圆 + 亮瞳孔
    le_x = cx - rng.randint(20, 28)
    le_y = cy - rng.randint(20, 30)
    eye_color = (rng.randint(5, 25), rng.randint(5, 25), rng.randint(5, 25))
    cv2.ellipse(img, (le_x, le_y), (10, 6), 0, 0, 360, eye_color, -1)
    cv2.circle(img, (le_x, le_y), 3, (240, 240, 255), -1)

    # 右眼
    re_x = cx + rng.randint(20, 28)
    re_y = cy - rng.randint(20, 30)
    cv2.ellipse(img, (re_x, re_y), (10, 6), 0, 0, 360, eye_color, -1)
    cv2.circle(img, (re_x, re_y), 3, (240, 240, 255), -1)

    # 鼻子
    nose = (rng.randint(30, 70), rng.randint(20, 50), rng.randint(15, 40))
    cv2.ellipse(img, (cx, cy + 8), (rng.randint(6, 10), rng.randint(10, 15)), 0, 0, 360, nose, -1)

    # 嘴 — 横线
    mouth_y = cy + rng.randint(32, 42)
    mouth_color = (rng.randint(10, 60), rng.randint(10, 40), rng.randint(30, 80))
    cv2.line(img, (cx - rng.randint(16, 22), mouth_y),
             (cx + rng.randint(16, 22), mouth_y), mouth_color, rng.randint(2, 3))

    # 眉毛 (增强眼部对比度)
    brow_color = (rng.randint(10, 30), rng.randint(10, 30), rng.randint(10, 30))
    cv2.line(img, (le_x - 10, le_y - 10), (le_x + 10, le_y - 10), brow_color, 2)
    cv2.line(img, (re_x - 10, re_y - 10), (re_x + 10, re_y - 10), brow_color, 2)

    # 纹理噪声 (同人同脸, 但 variant 不同 → 轻微差异)
    tex_rng = np.random.RandomState(seed + 777)
    noise = tex_rng.randint(0, 15, (canvas_size, canvas_size, 3), dtype=np.uint8)
    img = cv2.add(img, noise)

    return img


def make_aligned(person_seed: int, variant: int) -> np.ndarray:
    """生成预对齐 112×112 BGR 人脸 (跳过检测, 直接供 extract 用)。"""
    seed = person_seed * 1000 + variant
    rng = np.random.RandomState(seed)

    bg = rng.randint(190, 230)
    img = np.full((112, 112, 3), bg, dtype=np.uint8)
    cx, cy = 56, 56

    # 肤色椭圆 — 更小更精准 (112 画布上的人脸比例)
    skin = (rng.randint(50, 110), rng.randint(70, 130), rng.randint(90, 170))
    cv2.ellipse(img, (cx, cy), (30 + rng.randint(0, 8), 38 + rng.randint(0, 10)), 0, 0, 360, skin, -1)

    # 眼
    eye_c = (rng.randint(5, 20), rng.randint(5, 20), rng.randint(5, 20))
    cv2.ellipse(img, (cx - 12, cy - 10), (5, 3), 0, 0, 360, eye_c, -1)
    cv2.ellipse(img, (cx + 12, cy - 10), (5, 3), 0, 0, 360, eye_c, -1)

    # 鼻子
    nose = (rng.randint(30, 60), rng.randint(20, 50), rng.randint(15, 40))
    cv2.ellipse(img, (cx, cy + 5), (4, 7), 0, 0, 360, nose, -1)

    # 嘴
    m_c = (rng.randint(15, 60), rng.randint(10, 40), rng.randint(30, 80))
    cv2.line(img, (cx - 10, cy + 18), (cx + 10, cy + 18), m_c, 2)

    # 纹理
    tex_rng = np.random.RandomState(seed + 777)
    img = cv2.add(img, tex_rng.randint(0, 12, (112, 112, 3), dtype=np.uint8))
    return img


def main():
    os.makedirs(SAMPLES_DIR, exist_ok=True)
    os.makedirs(ALIGNED_DIR, exist_ok=True)

    for p in range(NUM_PERSONS):
        person_name = f"person_{p + 1:03d}"
        person_dir = os.path.join(SAMPLES_DIR, person_name)
        os.makedirs(person_dir, exist_ok=True)

        for v in range(IMAGES_PER_PERSON):
            # jpg 样本 (可被 preprocess 检测)
            img = draw_face_bgr(CANVAS, p, v)
            jpg_path = os.path.join(person_dir, f"{v + 1}.jpg")
            cv2.imwrite(jpg_path, img)

            # 预对齐 112×112 .npy (跳过检测, 直接给 extract)
            aligned = make_aligned(p, v)
            npy_path = os.path.join(ALIGNED_DIR, f"{person_name}_{v}.npy")
            np.save(npy_path, aligned)

    print(f"Generated {NUM_PERSONS} x {IMAGES_PER_PERSON} images in {SAMPLES_DIR}")
    print(f"Pre-aligned 112x112 .npy in {ALIGNED_DIR}")
    print(f"\n  samples/  → 供 Docker device 容器 preprocess")
    print(f"  aligned/  → 跳过检测, 直接供 edge 容器 extract")


if __name__ == "__main__":
    main()
