"""
图像级保护方法模块。
支持的策略：高斯模糊、像素化（马赛克）、区域遮罩。
"""

import cv2
import numpy as np


def apply_gaussian_blur(image: np.ndarray, kernel_size: int = 9) -> np.ndarray:
    """高斯模糊：σ = kernel_size / 3，核大小必须为奇数。"""
    k = max(3, kernel_size | 1)  # 确保奇数
    return cv2.GaussianBlur(image, (k, k), k / 3.0)


def apply_pixelate(image: np.ndarray, block_size: int = 8) -> np.ndarray:
    """
    像素化（马赛克）：将图像划分为 block×block 的块，每块用其均值填充。
    """
    h, w = image.shape[:2]
    bs = max(2, block_size)
    # 缩小再放大，实现马赛克效果
    small = cv2.resize(image, (max(1, w // bs), max(1, h // bs)), interpolation=cv2.INTER_LINEAR)
    return cv2.resize(small, (w, h), interpolation=cv2.INTER_NEAREST)


def apply_region_mask(image: np.ndarray, region: str = "eyes", mask_type: str = "black") -> np.ndarray:
    """
    区域遮罩：对指定面部区域进行遮盖。

    Args:
        image:  112×112 人脸图像
        region: eyes / eyes_nose / lower_face
        mask_type: black / blur / texture
    """
    h, w = image.shape[:2]
    result = image.copy()

    # 定义遮罩区域（112×112 模板中的近似位置）
    masks = {
        "eyes":       [(int(w*0.20), int(h*0.25), int(w*0.60), int(h*0.18))],
        "eyes_nose":  [(int(w*0.20), int(h*0.25), int(w*0.60), int(h*0.35))],
        "lower_face": [(int(w*0.15), int(h*0.55), int(w*0.70), int(h*0.40))],
    }
    regions = masks.get(region, masks["eyes"])

    for (rx, ry, rw, rh) in regions:
        if mask_type == "black":
            cv2.rectangle(result, (rx, ry), (rx + rw, ry + rh), (0, 0, 0), -1)
        elif mask_type == "blur":
            roi = result[ry:ry + rh, rx:rx + rw]
            result[ry:ry + rh, rx:rx + rw] = cv2.GaussianBlur(roi, (15, 15), 10)
        elif mask_type == "texture":
            roi = result[ry:ry + rh, rx:rx + rw]
            noise = np.random.randint(0, 256, roi.shape, dtype=np.uint8)
            result[ry:ry + rh, rx:rx + rw] = cv2.addWeighted(roi, 0.3, noise, 0.7, 0)

    return result


def protect(image: np.ndarray, method: str, **params) -> np.ndarray:
    """统一入口，根据 method 名调用对应的保护函数。"""
    if method == "gaussian":
        return apply_gaussian_blur(image, params.get("kernel_size", 9))
    elif method == "pixelate":
        return apply_pixelate(image, params.get("block_size", 8))
    elif method == "mask":
        return apply_region_mask(image,
                                  region=params.get("region", "eyes"),
                                  mask_type=params.get("mask_type", "black"))
    else:
        return image  # 不识别的方法 → 原图返回
