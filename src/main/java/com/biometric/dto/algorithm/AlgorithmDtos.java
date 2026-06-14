package com.biometric.dto.algorithm;

import java.util.List;
import java.util.Map;

/**
 * 统一算法接口契约 — Java DTO (阶段 0)。
 * <p>
 * 本文件<b>只定义契约数据结构</b>，不含任何业务逻辑。
 * 编排层(Spring Boot)按算法注册表的 serviceUrl + params 转发请求/解析响应，
 * 永不感知算法内部实现 —— 这是平台插件化的地基。
 * </p>
 * <p>
 * 对应文档: docs/algorithm-contract.md<br>
 * 对应 Python: simulator/biometric_algo/contract.py
 * </p>
 * <p>五类算法 (algoType):</p>
 * <ul>
 *   <li>preprocess           预处理(检测/对齐)      —— 终端层</li>
 *   <li>image_privacy        图像隐私保护(预处理后)  —— 边缘层</li>
 *   <li>feature_extractor    特征提取(模型槽位)      —— 边缘层</li>
 *   <li>template_protection  模板保护(提取后)        —— 边缘层</li>
 *   <li>matcher              匹配评测               —— 云端层</li>
 * </ul>
 * <p>不变量:</p>
 * <ol>
 *   <li>阶段间只传文件路径，不传数组本体</li>
 *   <li>每个响应必带 timeMs 真实耗时</li>
 *   <li>matcher 的 genuine/impostor 分数是 ROC/EER 的唯一合法来源</li>
 * </ol>
 */
public final class AlgorithmDtos {

    private AlgorithmDtos() {
        // 纯契约容器，禁止实例化
    }

    /** 五类算法类型。HTTP/JSON 中以小写下划线字符串传输。 */
    public enum AlgoType {
        PREPROCESS("preprocess"),
        IMAGE_PRIVACY("image_privacy"),
        FEATURE_EXTRACTOR("feature_extractor"),
        TEMPLATE_PROTECTION("template_protection"),
        MATCHER("matcher");

        private final String wire;

        AlgoType(String wire) {
            this.wire = wire;
        }

        /** JSON 线格式值，如 "feature_extractor"。 */
        public String wire() {
            return wire;
        }
    }

    /** 匹配度量方式。 */
    public enum Metric {
        COSINE("cosine"),     // 浮点嵌入 → 余弦相似度
        HAMMING("hamming");   // 二进制保护模板 → 归一化汉明相似度

        private final String wire;

        Metric(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    // ─────────────────────── 通用响应信封 ───────────────────────

    /**
     * 所有算法的统一响应信封 {code, msg, timeMs, result}。
     *
     * @param code   200=成功；非200=失败
     * @param msg    描述信息
     * @param timeMs 本阶段真实耗时(毫秒)，性能指标来源
     * @param result 各算法类型专属输出，键见各 *Result 记录
     */
    public record AlgoResponse(
            int code,
            String msg,
            double timeMs,
            Map<String, Object> result
    ) {}

    // ─────────────────────── 各类请求记录 ───────────────────────

    /** preprocess 请求。 */
    public record PreprocessRequest(
            String taskId,
            List<String> imagePaths,
            Map<String, Object> params
    ) {}

    /** image_privacy 请求。params 例: {"method":"gaussian","kernelSize":9} */
    public record ImagePrivacyRequest(
            String taskId,
            List<String> imagePaths,
            Map<String, Object> params
    ) {}

    /** feature_extractor 请求。params 例: {"modelName":"arcface_r100"} */
    public record FeatureExtractorRequest(
            String taskId,
            List<String> imagePaths,
            Map<String, Object> params
    ) {}

    /** template_protection 请求。params 例: {"method":"biohash","bitLength":256,"seed":42} */
    public record TemplateProtectionRequest(
            String taskId,
            List<String> featurePaths,
            Map<String, Object> params
    ) {}

    /** matcher 请求。 */
    public record MatcherRequest(
            String taskId,
            List<String> probePaths,
            List<String> galleryPaths,
            Metric metric,
            Map<String, Object> params
    ) {}

    // ─────────────────────── 各类 result 记录 ───────────────────────
    // 描述 AlgoResponse.result 的精确结构，供解析端取用。

    /** preprocess 输出：112×112 对齐人脸 (.npy) 路径。 */
    public record PreprocessResult(List<String> alignedPaths) {}

    /** image_privacy 输出：保护后图像 (.npy) 路径。 */
    public record ImagePrivacyResult(List<String> protectedPaths) {}

    /** feature_extractor 输出：特征嵌入 (.npy) 路径 + 维度。 */
    public record FeatureExtractorResult(List<String> featurePaths, int dim) {}

    /** template_protection 输出：保护后模板 (.npy) 路径。 */
    public record TemplateProtectionResult(List<String> templatePaths) {}

    /** matcher 输出：真实匹配分数 —— ROC/EER 唯一合法来源。 */
    public record MatcherResult(
            List<Double> genuineScores,
            List<Double> impostorScores
    ) {}

    // ─────────────────────── 注册表 / 参数描述 ───────────────────────

    /**
     * paramSchema 中的单个参数描述，供前端动态渲染表单。
     *
     * @param type int / float / string / enum / bool
     */
    public record ParamField(
            String key,
            String label,
            String type,
            Object defaultValue,
            Double min,
            Double max,
            Double step,
            List<Object> options
    ) {}

    /**
     * 算法注册表条目 (与 MySQL algorithm 表对应)。
     * 编排层按 serviceUrl + params 转发，永不感知算法内部实现。
     */
    public record AlgorithmSpec(
            String name,
            AlgoType type,
            String modality,
            String version,
            String serviceUrl,
            List<ParamField> paramSchema,
            Map<String, Object> ioSpec,
            String status
    ) {}
}
