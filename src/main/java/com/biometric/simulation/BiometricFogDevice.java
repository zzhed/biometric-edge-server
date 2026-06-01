package com.biometric.simulation;

import lombok.Getter;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * 生物特征边缘节点设备模型，适配自 EdgeWorkflow 的 FogDevice。
 * <p>
 * 每个 BiometricFogDevice 代表边缘计算中的一个层级节点（云端/边缘/终端），
 * 封装了 CloudSim 的 Datacenter，包含计算资源（MIPS）、能耗参数和网络带宽信息。
 * </p>
 */
@Getter
public class BiometricFogDevice {

    /** 层级：cloud / edge / device */
    private final String tier;

    /** 节点名称 */
    private final String name;

    /** 总计算能力（MIPS） */
    private final long totalMips;

    /** 主机数量 */
    private final int hostCount;

    /** 上行带宽（Mbps） */
    private final double uplinkBandwidth;

    /** 下行带宽（Mbps） */
    private final double downlinkBandwidth;

    /** 上行延迟（ms） */
    private final double uplinkLatency;

    /** 静态功耗（W） */
    private final double staticPower;

    /** 最大功耗（W） */
    private final double maxPower;

    /** CloudSim 数据中心 */
    private final Datacenter datacenter;

    /** 该设备下的主机列表 */
    private final List<HostSimple> hosts = new ArrayList<>();

    /** 该设备下的虚拟机列表 */
    private final List<Vm> vmList = new ArrayList<>();

    /**
     * 创建一个边缘设备节点。
     *
     * @param simulation    CloudSim 仿真引擎实例
     * @param name          节点名称（如 "Cloud-GPU"）
     * @param tier          层级（cloud / edge / device）
     * @param totalMips     总 MIPS 计算能力
     * @param hostCount     模拟的物理主机数量
     * @param uplinkBw      上行带宽（Mbps）
     * @param downlinkBw    下行带宽（Mbps）
     * @param uplinkLatency 上行延迟（ms）
     * @param staticPower   空闲功耗（W）
     * @param maxPower      满载功耗（W）
     */
    public BiometricFogDevice(CloudSim simulation, String name, String tier,
                              long totalMips, int hostCount,
                              double uplinkBw, double downlinkBw, double uplinkLatency,
                              double staticPower, double maxPower) {
        this.name = name;
        this.tier = tier;
        this.totalMips = totalMips;
        this.hostCount = hostCount;
        this.uplinkBandwidth = uplinkBw;
        this.downlinkBandwidth = downlinkBw;
        this.uplinkLatency = uplinkLatency;
        this.staticPower = staticPower;
        this.maxPower = maxPower;

        // 创建物理主机列表
        long mipsPerHost = totalMips / hostCount;
        for (int i = 0; i < hostCount; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(mipsPerHost / 4));
            }
            HostSimple host = new HostSimple(4096, 100_000, 1_000_000, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            this.hosts.add(host);
        }

        // 创建数据中心
        this.datacenter = new DatacenterSimple(simulation, this.hosts);
    }

    /**
     * 在该设备上创建一台虚拟机。
     *
     * @param vmMips 分配给虚拟机的 MIPS
     * @return 创建的虚拟机
     */
    public Vm createVm(long vmMips) {
        Vm vm = new VmSimple(vmMips, 1);
        vm.setRam(1024)
          .setBw(1000)
          .setSize(10_000)
          .setCloudletScheduler(new CloudletSchedulerTimeShared());
        vmList.add(vm);
        return vm;
    }

    /**
     * 创建模拟样本任务（同步模式，不经过 Broker）。
     * 直接在目标 VM 的调度器中模拟执行。
     *
     * @param vm          目标虚拟机
     * @param workloadMi  计算工作量（MI）
     * @return 预估执行时间（ms）
     */
    public long simulateExecution(Vm vm, long workloadMi) {
        // 计算执行时间 = 工作量(MI) / VM的MIPS * 1000 (ms)
        return (long) ((double) workloadMi / vm.getMips() * 1000.0);
    }

    /**
     * 估算指定任务类型所需的计算工作量（MI）。
     *
     * @param taskType    任务类型
     * @param sampleCount 样本数量
     * @return 工作量（MI）
     */
    public static long estimateWorkload(String taskType, int sampleCount) {
        // 单样本基准 MI 值（基于经验估算，可通过 DockerLink 校准）
        double baseMi = switch (taskType) {
            case "preprocess"       -> 500;    // MTCNN 人脸检测
            case "extract"          -> 2000;   // ArcFace 特征提取
            case "protect"          -> 200;    // 模板/图像保护
            case "protect_image"    -> 200;
            case "protect_template" -> 200;
            case "match"            -> 50;     // 余弦相似度匹配
            default                 -> 500;
        };
        return (long) (baseMi * sampleCount);
    }
}
