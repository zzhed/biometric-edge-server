"""
生物特征识别算法模块。
每个模块独立实现一个管道阶段的真实计算逻辑。

当前状态：
  preprocess       ✅ OpenCV Haar 级联人脸检测 + 对齐
  extract          ⚠️ 模拟 embedding（占位，待替换为 ArcFace ONNX）
  protect_image    ✅ 高斯模糊 / 像素化 / 区域遮罩
  protect_template ✅ BioHash 随机投影 + 二值化
  match            ✅ 余弦相似度 / 汉明距离
"""
