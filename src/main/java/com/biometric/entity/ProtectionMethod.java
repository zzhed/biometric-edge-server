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
public class ProtectionMethod {
    private Long id;
    private String name;
    private String level;
    private String type;
    private String description;
    private String parameters;  // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
