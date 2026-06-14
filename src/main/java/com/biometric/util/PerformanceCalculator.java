package com.biometric.util;

/**
 * 性能计算器 —— 能耗/延迟指标 (从 BiometricCloudSimEngine 抽离，删除 CloudSim 依赖)。
 * <p>
 * 延迟 = Docker 容器真实墙钟测量值 (timeMs)，不再由任何模拟器编造。
 * 能耗 = 文献典型功耗参数 × 实测时间，使用经典的 linear power model:
 * <pre>
 *   E = P_static × T_total + (P_max - P_static) × utilization × T_compute + net × T_network
 * </pre>
 * 功耗参数取值参考文献 [TODO: 引用边缘计算文献]:
 *   device (RPi-class): static=5W, max=15W
 *   edge   (Jetson-class): static=30W, max=80W
 *   cloud  (server-class): static=200W, max=400W
 * </p>
 */
public final class PerformanceCalculator {

    private PerformanceCalculator() { /* 纯工具类，禁止实例化 */ }

    /** 每层预设功耗参数 (device / edge / cloud) */
    public static final class TierPower {
        public final double staticPowerW;
        public final double maxPowerW;
        public final double networkCostW;  // 每 ms 网络传输的能耗 (W)

        public TierPower(double staticPowerW, double maxPowerW, double networkCostW) {
            this.staticPowerW = staticPowerW;
            this.maxPowerW = maxPowerW;
            this.networkCostW = networkCostW;
        }
    }

    public static TierPower forTier(String tier) {
        return switch (tier) {
            case "cloud"  -> new TierPower(200.0, 400.0, 0.125);
            case "edge"   -> new TierPower(30.0,   80.0, 0.125);
            case "device" -> new TierPower(5.0,    15.0, 0.125);
            default       -> new TierPower(30.0,   80.0, 0.125);
        };
    }

    /**
     * 计算单阶段能耗 (mJ)。
     *
     * @param timeMs          本阶段总耗时(含网络), 来自 Docker 实测
     * @param computeMs       纯计算耗时(不含网络), 来自 Docker 实测
     * @param networkLatencyMs 网络延迟, 来自 Docker 实测; 0=本阶段无跨容器传输
     * @param tier             层级: device / edge / cloud
     * @param utilization      计算期间 CPU 利用率, 默认 0.8
     * @return 能耗 (mJ)
     */
    public static double energyMj(long timeMs, long computeMs, long networkLatencyMs,
                                   String tier, double utilization) {
        TierPower p = forTier(tier);
        double totalSec   = timeMs / 1000.0;
        double computeSec = (computeMs > 0 ? computeMs : timeMs) / 1000.0;

        double energyJ = p.staticPowerW * totalSec
                + (p.maxPowerW - p.staticPowerW) * utilization * computeSec;
        if (networkLatencyMs > 0) {
            energyJ += p.networkCostW * networkLatencyMs / 1000.0;
        }
        return Math.round(energyJ * 1000.0 * 100.0) / 100.0;  // J → mJ, 保留 2 位
    }

    /**
     * 估算某阶段的计算工作量 (MI)，用于日志/展示，不再用于模拟。
     * 保留此方法是为了在前端展示"这个阶段大概有多重"——完全可删除，不影响核心指标。
     */
    public static long estimateWorkload(String stage, int samples) {
        double base = switch (stage) {
            case "preprocess"       -> 500;
            case "protect_image"    -> 800;
            case "extract"          -> 3000;
            case "protect_template" -> 200;
            case "match"            -> 150;
            default                 -> 1000;
        };
        return (long) (base * samples);
    }
}
