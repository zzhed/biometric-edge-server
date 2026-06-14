"""
统一算法接口契约 — Pydantic 模型 (阶段 0)
==========================================
本模块**只定义契约数据结构**，不含任何业务逻辑。

对应文档: docs/algorithm-contract.md
对应 Java: src/main/java/com/biometric/dto/algorithm/AlgorithmDtos.java

五类算法 (algoType):
    preprocess           预处理(检测/对齐)      —— 终端层
    image_privacy        图像隐私保护(预处理后)  —— 边缘层
    feature_extractor    特征提取(模型槽位)      —— 边缘层
    template_protection  模板保护(提取后)        —— 边缘层
    matcher              匹配评测               —— 云端层

不变量:
    1. 阶段间只传文件路径, 不传数组本体
    2. 每个响应必带 timeMs 真实耗时
    3. matcher 的 genuine/impostor 分数是 ROC/EER 的唯一合法来源
"""

from __future__ import annotations

from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


# ─────────────────────────── 枚举 ───────────────────────────

class AlgoType(str, Enum):
    """五类算法类型。"""
    PREPROCESS = "preprocess"
    IMAGE_PRIVACY = "image_privacy"
    FEATURE_EXTRACTOR = "feature_extractor"
    TEMPLATE_PROTECTION = "template_protection"
    MATCHER = "matcher"


class Metric(str, Enum):
    """匹配度量方式。"""
    COSINE = "cosine"      # 浮点嵌入 → 余弦相似度
    HAMMING = "hamming"    # 二进制保护模板 → 归一化汉明相似度


# ─────────────────────── 通用响应信封 ───────────────────────

class AlgoResponse(BaseModel):
    """
    所有算法的统一响应信封 {code, msg, timeMs, result}。

    result 的具体结构由各算法类型决定 (见下方 *Result 模型),
    此处用 dict 承载以保持端点通用; 具体端点可返回更精确的子模型。
    """
    code: int = 200
    msg: str = "success"
    timeMs: float = Field(0.0, description="本阶段真实耗时(毫秒), 性能指标来源")
    result: dict[str, Any] = Field(default_factory=dict)


# ─────────────────────── 各类请求模型 ───────────────────────

class PreprocessRequest(BaseModel):
    taskId: str
    algoType: AlgoType = AlgoType.PREPROCESS
    imagePaths: list[str]
    params: dict[str, Any] = Field(default_factory=dict)


class ImagePrivacyRequest(BaseModel):
    taskId: str
    algoType: AlgoType = AlgoType.IMAGE_PRIVACY
    imagePaths: list[str]
    # 例: {"method": "gaussian", "kernelSize": 9}
    params: dict[str, Any] = Field(default_factory=dict)


class FeatureExtractorRequest(BaseModel):
    taskId: str
    algoType: AlgoType = AlgoType.FEATURE_EXTRACTOR
    imagePaths: list[str]
    # 例: {"modelName": "arcface_r100"}
    params: dict[str, Any] = Field(default_factory=dict)


class TemplateProtectionRequest(BaseModel):
    taskId: str
    algoType: AlgoType = AlgoType.TEMPLATE_PROTECTION
    featurePaths: list[str]
    # 例: {"method": "biohash", "bitLength": 256, "seed": 42}
    params: dict[str, Any] = Field(default_factory=dict)


class MatcherRequest(BaseModel):
    taskId: str
    algoType: AlgoType = AlgoType.MATCHER
    probePaths: list[str]
    galleryPaths: list[str]
    metric: Metric = Metric.COSINE
    params: dict[str, Any] = Field(default_factory=dict)


# ─────────────────────── 各类 result 模型 ───────────────────────
# 这些模型描述 AlgoResponse.result 的精确结构, 供实现端构造/校验。

class PreprocessResult(BaseModel):
    alignedPaths: list[str]          # 112×112 对齐人脸 (.npy)


class ImagePrivacyResult(BaseModel):
    protectedPaths: list[str]        # 保护后图像 (.npy)


class FeatureExtractorResult(BaseModel):
    featurePaths: list[str]          # 特征嵌入 (.npy)
    dim: int                         # 嵌入维度, 如 512


class TemplateProtectionResult(BaseModel):
    templatePaths: list[str]         # 保护后模板 (.npy)


class MatcherResult(BaseModel):
    genuineScores: list[float]       # 同人匹配分数 —— ROC/EER 来源
    impostorScores: list[float]      # 异人匹配分数 —— ROC/EER 来源


# ─────────────────────── 注册表 / 参数描述 ───────────────────────

class ParamField(BaseModel):
    """
    paramSchema 中的单个参数描述, 供前端动态渲染表单。
    type: int / float / string / enum / bool
    """
    key: str
    label: str
    type: str
    default: Optional[Any] = None
    min: Optional[float] = None
    max: Optional[float] = None
    step: Optional[float] = None
    options: Optional[list[Any]] = None   # type=enum 时的可选值


class AlgorithmSpec(BaseModel):
    """
    算法注册表条目 (与 MySQL algorithm 表对应)。
    编排层按 serviceUrl + params 转发, 永不感知算法内部实现。
    """
    name: str
    type: AlgoType
    modality: str = "face"
    version: str = "v1"
    serviceUrl: str
    paramSchema: list[ParamField] = Field(default_factory=list)
    ioSpec: dict[str, Any] = Field(default_factory=dict)
    status: str = "enabled"


__all__ = [
    "AlgoType", "Metric", "AlgoResponse",
    "PreprocessRequest", "ImagePrivacyRequest", "FeatureExtractorRequest",
    "TemplateProtectionRequest", "MatcherRequest",
    "PreprocessResult", "ImagePrivacyResult", "FeatureExtractorResult",
    "TemplateProtectionResult", "MatcherResult",
    "ParamField", "AlgorithmSpec",
]
