"""
模板级保护方法模块。
BioHash：基于随机投影矩阵 + 阈值二值化，生成可撤销的二进制生物特征模板。

原理：
  1. embedding (512维 float32) × 随机投影矩阵 (512×N) → N 维向量
  2. 二值化：>0 → 1, ≤0 → 0
  3. 得到 N bits 的二进制模板（原始特征无法从 hash 逆推）

种子固定时，投影矩阵可复现；更换种子即生成完全不同的模板（可撤销性）。
"""

import numpy as np


# 缓存投影矩阵（避免每次调用都做 QR 分解）
_proj_cache: dict[tuple[int, int, int], np.ndarray] = {}


def _get_projection_matrix(dim: int, bit_length: int, seed: int) -> np.ndarray:
    """获取或创建正交化随机投影矩阵（带缓存）。"""
    key = (dim, bit_length, seed)
    if key not in _proj_cache:
        rng = np.random.RandomState(seed)
        proj = rng.randn(dim, bit_length).astype(np.float32)
        proj, _ = np.linalg.qr(proj)
        _proj_cache[key] = proj
    return _proj_cache[key]


def biohash(embeddings: list[np.ndarray], bit_length: int = 256, seed: int = 42) -> list[np.ndarray]:
    """
    对嵌入向量执行 BioHash 保护。

    Args:
        embeddings: L2 归一化的嵌入向量列表 (N, 512)
        bit_length: 输出二进制模板的位数
        seed:       随机种子（改变种子 = 撤销旧模板、生成新模板）

    Returns:
        二进制保护模板列表 (N, bit_length)，dtype=uint8
    """
    if not embeddings:
        return []

    dim = embeddings[0].shape[0]  # 应为 512
    proj = _get_projection_matrix(dim, bit_length, seed)

    templates = []
    for emb in embeddings:
        # 投影：512 → N
        projected = emb @ proj  # (512,) @ (512, N) → (N,)
        # 二值化
        binary = (projected > 0).astype(np.uint8)
        templates.append(binary)

    return templates


def biohash_single(embedding: np.ndarray, bit_length: int = 256, seed: int = 42) -> np.ndarray:
    """对单个嵌入向量执行 BioHash。"""
    result = biohash([embedding], bit_length, seed)
    return result[0] if result else np.zeros(bit_length, dtype=np.uint8)


def aead_encrypt(embedding: np.ndarray, key: bytes = b"biometric-key-32b!") -> bytes:
    """
    AES-256-GCM 加密（模拟真实加密的占位实现）。
    """
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    aesgcm = AESGCM(key.ljust(32, b'\0')[:32])
    nonce = np.random.bytes(12)
    plaintext = embedding.tobytes()
    ciphertext = aesgcm.encrypt(nonce, plaintext, None)
    return nonce + ciphertext  # 前缀 nonce
