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
public class DatasetInfo {
    private Long id;
    private String name;
    private String modality;
    private Integer sampleCount;
    private String description;
    private LocalDateTime createdAt;
}
