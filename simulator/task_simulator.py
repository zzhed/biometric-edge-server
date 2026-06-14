"""
FastAPI 生物特征推理服务 (阶段 1 串流版)。
部署在每个 Docker 容器内（Cloud / Edge / Device），对外提供 /health 和 /execute 端点。

串流机制:
  所有阶段通过共享数据目录 DATA_DIR (默认 /data) 的文件路径传递中间产物。
  阶段间传输的是路径字符串，不传数组本体。
  outputPaths 写入 DATA_DIR 下按 stage 命名的子目录中。

执行流程:
  preprocess       → 读 imagePaths → OpenCV 人脸检测+对齐 → 写 alignedPaths (.npy)
  protect_image    → 读 imagePaths → 图像保护 → 写 protectedPaths (.npy)
  extract          → 读 imagePaths → 特征提取 → 写 featurePaths (.npy)
  protect_template → 读 featurePaths → 模板保护 → 写 templatePaths (.npy)
  match            → 读 probePaths + galleryPaths → 匹配分数 → 返回 genuine/impostor scores
"""
import os
import time
import random
import numpy as np
import cv2
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional

from biometric_algo import preprocess, extract, protect_image, protect_template, match as match_algo

NODE_TYPE = os.environ.get("NODE_TYPE", "edge")
MIPS      = int(os.environ.get("MIPS", "1000"))
PORT      = int(os.environ.get("PORT", "5001"))
DATA_DIR  = os.environ.get("DATA_DIR", "/data")

# 每层节点静态功耗 (W)
STATIC_POWER = {"cloud": 200.0, "edge": 30.0, "device": 5.0}
# 利用率假设
UTILIZATION = 0.8

os.makedirs(DATA_DIR, exist_ok=True)


# ────────── 文件 I/O 辅助 ──────────

def _ensure_stage_dir(stage: str) -> str:
    """创建并返回 /data/<stage>/ 目录。"""
    d = os.path.join(DATA_DIR, stage)
    os.makedirs(d, exist_ok=True)
    return d


def _save_images(images: list[np.ndarray], stage: str) -> list[str]:
    """保存图像列表为 .png，返回路径列表。"""
    d = _ensure_stage_dir(stage)
    paths = []
    for i, img in enumerate(images):
        p = os.path.join(d, f"{i}.png")
        cv2.imwrite(p, img)
        paths.append(p)
    return paths


def _save_arrays(arrays: list[np.ndarray], stage: str) -> list[str]:
    """保存数组列表为 .npy，返回路径列表。"""
    d = _ensure_stage_dir(stage)
    paths = []
    for i, arr in enumerate(arrays):
        p = os.path.join(d, f"{i}.npy")
        np.save(p, arr)
        paths.append(p)
    return paths


def _load_images(paths: list[str]) -> list[np.ndarray]:
    """加载图像文件列表。支持 .png/.jpg 和 .npy。"""
    images = []
    for p in paths:
        if p.endswith(".npy"):
            images.append(np.load(p))
        else:
            img = cv2.imread(p)
            if img is None:
                raise FileNotFoundError(f"无法读取图像: {p}")
            images.append(img)
    return images


def _load_arrays(paths: list[str]) -> list[np.ndarray]:
    """加载 .npy 数组文件列表。"""
    return [np.load(p) for p in paths]


# ────────── 能耗估算 ──────────

def _energy_mj(elapsed_ms: float) -> float:
    """根据实测耗时和节点功耗参数估算能耗 (mJ)。"""
    static_w = STATIC_POWER.get(NODE_TYPE, 30.0)
    total_sec = elapsed_ms / 1000.0
    energy = static_w * total_sec + static_w * UTILIZATION * total_sec * 0.85
    return round(energy, 2)


# ────────── 请求模型 ──────────

class ExecuteRequest(BaseModel):
    taskType: str = "extract"
    workload: int = 100
    inputPaths: Optional[list[str]] = None      # 上游阶段的输出路径
    probePaths: Optional[list[str]] = None       # match 专用: probe 模板路径
    galleryPaths: Optional[list[str]] = None     # match 专用: gallery 模板路径
    params: Optional[dict] = None                # 算法参数 (方法名/bitLength/seed 等)


# ────────── FastAPI 应用 ──────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    print(f"[BiometricService] {NODE_TYPE.upper()} node started, MIPS={MIPS}, port={PORT}, data={DATA_DIR}")
    yield


app = FastAPI(title=f"BiometricEdgeService-{NODE_TYPE}", version="3.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


@app.get("/health")
def health():
    return {"nodeType": NODE_TYPE, "mips": MIPS, "cpuUsage": round(random.uniform(5, 35), 1), "status": "online"}


@app.post("/execute")
def execute(req: ExecuteRequest):
    t0 = time.perf_counter()
    task_type = req.taskType
    count = max(1, req.workload)
    params = req.params or {}

    try:
        # ──── preprocess: 图像 → 检测 + 对齐 → aligned paths ────
        if task_type == "preprocess":
            if req.inputPaths:
                images = _load_images(req.inputPaths)
            else:
                # 没有输入路径时使用测试虚拟图像（阶段 1 过渡用，阶段 2 移除）
                images = [_dummy_face_image() for _ in range(min(count, 20))]
            faces = preprocess.preprocess_batch(images)
            paths = _save_images(faces, "aligned")
            result = {"alignedPaths": paths, "faceCount": len(faces)}

        # ──── image_privacy: 对齐人脸 → 保护 → protected paths ────
        elif task_type == "protect_image":
            if req.inputPaths:
                images = _load_images(req.inputPaths)
            else:
                images = [_dummy_face_image() for _ in range(min(count, 10))]
            method = params.get("method", "gaussian")
            kwargs = {k: v for k, v in params.items() if k != "method"}
            protected = [protect_image.protect(img, method, **kwargs) for img in images]
            paths = _save_images(protected, "protected")
            result = {"protectedPaths": paths, "method": method, "applied": True}

        # ──── feature_extractor: 图像 → embedding → feature paths ────
        elif task_type == "extract":
            if req.inputPaths:
                images = _load_images(req.inputPaths)
            else:
                images = [_dummy_face_image() for _ in range(min(count, 20))]
            embeddings = extract.extract(images)
            paths = _save_arrays(embeddings, "features")
            result = {"featurePaths": paths, "dim": int(embeddings[0].shape[0]) if embeddings else 0, "count": len(embeddings)}

        # ──── template_protection: embedding → 保护模板 → template paths ────
        elif task_type == "protect_template":
            if req.inputPaths:
                embeddings = _load_arrays(req.inputPaths)
            else:
                rng = np.random.RandomState(42)
                emb = rng.randn(512).astype(np.float32)
                emb /= np.linalg.norm(emb)
                embeddings = [emb] * min(count, 50)
            method = params.get("method", "biohash")
            bit_length = params.get("bitLength", params.get("bit_length", 256))
            seed = params.get("seed", 42)
            templates = protect_template.biohash(embeddings, bit_length=bit_length, seed=seed)
            paths = _save_arrays(templates, "templates")
            result = {"templatePaths": paths, "method": method, "bitLength": bit_length, "count": len(templates)}

        # ──── match: probe + gallery templates → genuine/impostor 分数 ────
        elif task_type == "match":
            if req.probePaths and req.galleryPaths:
                probes   = _load_arrays(req.probePaths)
                galleries = _load_arrays(req.galleryPaths)
            elif req.inputPaths:
                # inputPaths 做简易分组: 前半探头, 后半图库
                all_data = _load_arrays(req.inputPaths)
                mid = max(1, len(all_data) // 2)
                probes, galleries = all_data[:mid], all_data[mid:]
            else:
                # 无任何输入: 用随机假数据作为最后兜底
                rng = np.random.RandomState(42)
                probes   = [rng.randn(512).astype(np.float32) for _ in range(min(count, 20))]
                galleries = [rng.randn(512).astype(np.float32) for _ in range(min(count, 20))]
                for v in probes + galleries: v /= np.linalg.norm(v)

            protected = params.get("protected", task_type == "protect_template")
            metric = params.get("metric", "cosine")
            genuine, impostor = match_algo.match_batch(probes, galleries, metric=metric)
            result = {"genuineScores": [round(s, 6) for s in genuine],
                       "impostorScores": [round(s, 6) for s in impostor],
                       "metric": metric,
                       "pairCount": len(genuine)}

        else:
            result = {"error": f"unknown taskType: {task_type}"}

    except Exception as e:
        result = {"error": str(e)}

    elapsed = (time.perf_counter() - t0) * 1000.0
    energy = _energy_mj(elapsed)

    return {
        "status": "success",
        "nodeType": NODE_TYPE,
        "taskType": task_type,
        "latencyMs": round(elapsed, 1),
        "energyMj": energy,
        "result": result,
    }


# ────────── 过渡用虚拟图像 (阶段 1 有输入路径时用不上, 落单时兜底) ──────────

def _dummy_face_image() -> np.ndarray:
    rng = np.random.RandomState(42)
    return (rng.rand(112, 112, 3) * 255).astype(np.uint8)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT, log_level="warning")
