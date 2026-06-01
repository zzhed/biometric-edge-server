package com.biometric.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeNode {
    private Long id;
    private String name;
    private String tier;
    private String host;
    private Integer port;
    private Integer mips;
    private String status;
    private Double cpuUsage;
    private LocalDateTime createdAt;
}
