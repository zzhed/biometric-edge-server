"""
特征提取模块 —— ArcFace (insightface / ONNX Runtime)。
从 112×112 BGR 对齐人脸提取 512 维 L2 归一化嵌入向量。

函数签名保持不变: extract(faces: list[np.ndarray]) → list[np.ndarray]
内部实现从伪随机嵌入替换为真实 ArcFace ONNX 推理。
"""

import hashlib
import numpy as np

EMBEDDING_DIM = 512  # ArcFace 标准输出维度

# ── ArcFace 模型单例 (惰性加载) ──
_recognizer = None
_recognizer_available = None  # None=未检测, True=可用, False=不可用

# 模型查找路径, 按优先级:
# insightface 规则: root='/data' → 查找 /data/models/buffalo_l/
#   ① /data                    ← Docker 共享卷 (data/models/buffalo_l/ 已预下载)
#   ② ~/.insightface           ← insightface 自动下载的默认位置
_MODEL_ROOTS = ["/data", None]  # None = insightface 默认路径 (~/.insightface)


def _get_recognizer():
    """惰性加载 insightface ArcFace 识别模型 (线程不安全, FastAPI 串行调用足够)。
    依次尝试 /data/models/ (共享卷) 和 ~/.insightface/models/ (本地), 取先找到的。
    """
    global _recognizer, _recognizer_available
    if _recognizer_available is None:
        import insightface
        for root in _MODEL_ROOTS:
            try:
                kwargs = dict(name='buffalo_l') if root is None else dict(name='buffalo_l', root=root)
                _recognizer = insightface.model_zoo.get_model(**kwargs)
                if _recognizer is not None:
                    _recognizer.prepare(ctx_id=-1)
                    _recognizer_available = True
                    print(f"[extract] ArcFace 模型已加载 (root={root or '默认'})")
                    break
            except Exception as e:
                print(f"[extract] root={root or '默认'} 加载失败: {e}")
                continue
        if _recognizer is None:
            _recognizer_available = False
            print("[extract] ArcFace 模型加载失败, 回退为伪嵌入。"
                  " 请将 buffalo_l 模型放至 simulator/data/models/ 或 pip install insightface")
    return _recognizer if _recognizer_available else None


def extract(faces: list[np.ndarray]) -> list[np.ndarray]:
    """
    从 112×112 BGR 人脸图像提取 512 维归一化嵌入向量。

    优先使用 insightface ArcFace ONNX 模型 (真实推理);
    若模型不可用则回退为基于图像哈希的确定性伪嵌入 (可复现但非真实识别)。

    Args:
        faces: 112×112 BGR 人脸图像列表

    Returns:
        L2 归一化的 float32 嵌入向量列表
    """
    recognizer = _get_recognizer()

    # ── 真实路径: ArcFace ONNX ──
    if recognizer is not None:
        embeddings = []
        for face in faces:
            emb = recognizer.get_feat(face)
            if emb is not None:
                emb = np.squeeze(emb).astype(np.float32)     # (1,512) → (512,)
                norm = np.linalg.norm(emb)
                if norm > 0 and abs(norm - 1.0) > 0.01:
                    emb /= norm                              # L2 归一化
                embeddings.append(emb)
            else:
                embeddings.append(np.zeros(EMBEDDING_DIM, dtype=np.float32))
        return embeddings

    # ── 回退: 确定性伪嵌入 (模型不可用时) ──
    embeddings = []
    for face in faces:
        h = hashlib.md5(face.tobytes()).digest()
        seed = int.from_bytes(h[:8], "big") % (2**31)
        rng = np.random.RandomState(seed)
        emb = rng.randn(EMBEDDING_DIM) * 0.3
        active = rng.rand(EMBEDDING_DIM) < 0.2
        emb[active] += rng.randn(active.sum()) * 1.0
        norm = np.linalg.norm(emb)
        if norm > 0:
            emb /= norm
        embeddings.append(emb.astype(np.float32))
    return embeddings


def extract_single(face: np.ndarray) -> np.ndarray:
    """提取单张人脸的嵌入。"""
    result = extract([face])
    return result[0] if result else np.zeros(EMBEDDING_DIM, dtype=np.float32)
