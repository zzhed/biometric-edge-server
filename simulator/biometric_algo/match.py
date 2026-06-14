"""
匹配模块。
支持余弦相似度（浮点嵌入）和汉明距离（二进制保护模板）。
"""

import numpy as np


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """
    两个嵌入向量的余弦相似度 [0, 1]。

    输入应为 L2 归一化向量，此时余弦相似度 = 点积。
    """
    return float(np.dot(a, b))


def hamming_distance(a: np.ndarray, b: np.ndarray) -> float:
    """
    两个二进制模板的归一化汉明距离 [0, 1]。
    0 = 完全相同，1 = 完全不匹配。
    """
    return float(np.count_nonzero(a != b)) / len(a)


def match_pair(
    probe: np.ndarray,
    gallery: np.ndarray,
    metric: str = "cosine",
    threshold: float = 0.5,
    protected: bool = False,
) -> dict:
    """
    比较一对特征向量。

    Args:
        probe:    探测向量
        gallery:  基准向量
        metric:   cosine / hamming
        threshold: 判定阈值
        protected: 是否使用二进制保护模板（汉明距离）

    Returns:
        { score, match, metric }
    """
    if protected or metric == "hamming":
        dist = hamming_distance(probe, gallery)
        score = 1.0 - dist   # 距离 → 相似度
    else:
        score = cosine_similarity(probe, gallery)

    return {
        "score": round(score, 6),
        "match": score >= threshold,
        "metric": metric,
    }


def match_batch(
    probes: list[np.ndarray],
    galleries: list[np.ndarray],
    metric: str = "cosine",
    threshold: float = 0.5,
) -> tuple[list[float], list[float]]:
    """
    批量匹配，返回 genuine 和 impostor 分数。

    Args:
        probes:    探测向量列表
        galleries: 基准向量列表
        metric:    cosine / hamming

    Returns:
        (genuine_scores, impostor_scores)
    """
    n = len(probes)
    genuine = []
    impostor = []

    for i in range(n):
        # Genuine: probe[i] vs gallery[i]（同一人）
        g = match_pair(probes[i], galleries[i % len(galleries)], metric, threshold, protected=False)
        genuine.append(g["score"])
        # Impostor: probe[i] vs gallery[(i+1)%n]（不同人）
        imp = match_pair(probes[i], galleries[(i + 1) % len(galleries)], metric, threshold, protected=False)
        impostor.append(imp["score"])

    return genuine, impostor
