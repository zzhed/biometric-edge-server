"""
预处理模块: insightface SCRFD 人脸检测 + 5点关键点仿射对齐到 112×112。

智能策略:
  · 如果输入已经是近似人脸裁剪 (小图 + 近方形) → 直接缩放,跳过检测
  · 否则 → SCRFD 检测 + 关键点对齐

SCRFD 模型来自 /data/models/buffalo_l/det_10g.onnx (已有 buffalo_l 包)。
"""

import cv2
import numpy as np

TARGET_SIZE = 112

STANDARD_LANDMARKS = np.array([
    [38.2946, 51.6963],   # left_eye
    [73.5318, 51.6963],   # right_eye
    [56.0252, 71.7366],   # nose
    [41.5493, 92.3655],   # left_mouth
    [70.7299, 92.3655],   # right_mouth
], dtype=np.float32)

# ── SCRFD 单例 (惰性加载) ──
_detector = None
_detector_ready = None
_MODEL_ROOTS = ["/data", None]


def _get_detector():
    """惰性加载 insightface FaceAnalysis (仅检测, 不计算 embedding)。"""
    global _detector, _detector_ready
    if _detector_ready is None:
        from insightface.app import FaceAnalysis
        for root in _MODEL_ROOTS:
            try:
                kwargs = dict(name="buffalo_l") if root is None else dict(name="buffalo_l", root=root)
                app = FaceAnalysis(**kwargs)
                app.prepare(ctx_id=-1, det_size=(640, 640), det_thresh=0.3)
                _detector = app
                _detector_ready = True
                print(f"[preprocess] SCRFD 检测器已加载 (root={root or '默认'})")
                break
            except Exception as e:
                print(f"[preprocess] root={root or '默认'} 失败: {e}")
                continue
        if _detector is None:
            _detector_ready = False
            print("[preprocess] SCRFD 不可用, 所有大图走 Haar 回退")
    return _detector if _detector_ready else None


def _is_face_crop(h: int, w: int) -> bool:
    """判断是否已是裁剪好的人脸: 小图 + 近似方形(0.7~1.4 宽高比)"""
    return max(h, w) < 250 and 0.5 < w / h < 2.0


def detect_and_align(image_bgr: np.ndarray) -> list[np.ndarray]:
    """
    检测并对齐图像中的所有人脸。

    Args:
        image_bgr: BGR 图像 (H, W, 3)

    Returns:
        对齐后的 112×112 BGR 人脸列表
    """
    if image_bgr is None or image_bgr.size == 0:
        return []

    h, w = image_bgr.shape[:2]

    # ── 策略 A: 已是裁剪人脸 → 直接缩放 ──
    if _is_face_crop(h, w):
        return [cv2.resize(image_bgr, (TARGET_SIZE, TARGET_SIZE), interpolation=cv2.INTER_CUBIC)]

    # ── 策略 B: 大图 → SCRFD 检测 + 对齐 ──
    app = _get_detector()
    if app is not None:
        faces = app.get(image_bgr)
        if faces:
            return _align_faces(image_bgr, [f for f in faces if f.kps is not None and f.bbox is not None])

    # ── 策略 C: SCRFD 不可用 → Haar 回退 ──
    detector = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
    gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)
    faces = detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
    results = []
    for (x, y, fw, fh) in faces:
        src = np.array([
            [x + fw * 0.30, y + fh * 0.35],
            [x + fw * 0.70, y + fh * 0.35],
            [x + fw * 0.50, y + fh * 0.55],
            [x + fw * 0.35, y + fh * 0.75],
            [x + fw * 0.65, y + fh * 0.75],
        ], dtype=np.float32)
        bbox = np.array([x, y, x + fw, y + fh], dtype=np.float32)
        results.append((bbox, src))
    return _align_faces(image_bgr, results)


def _align_faces(image_bgr: np.ndarray, face_data: list) -> list[np.ndarray]:
    """将 (bbox, kps) 列表对齐为 112×112 人脸。"""
    aligned = []
    for bbox, kps in face_data:
        src = kps[:5].astype(np.float32)
        M, _ = cv2.estimateAffinePartial2D(src, STANDARD_LANDMARKS)
        if M is not None:
            aligned.append(cv2.warpAffine(image_bgr, M, (TARGET_SIZE, TARGET_SIZE)))
        else:
            x1, y1, x2, y2 = np.int32(bbox)
            crop = image_bgr[max(0, y1):y2, max(0, x1):x2]
            if crop.size > 0:
                aligned.append(cv2.resize(crop, (TARGET_SIZE, TARGET_SIZE)))
    return aligned


def preprocess_batch(images: list[np.ndarray]) -> list[np.ndarray]:
    """对一批图像执行检测 + 对齐，返回所有检测到的人脸。"""
    all_faces = []
    for img in images:
        all_faces.extend(detect_and_align(img))
    return all_faces
