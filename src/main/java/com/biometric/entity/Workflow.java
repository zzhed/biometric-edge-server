package com.biometric.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {
    private Long id;
    private String name;
    private String nodes;
    private String edges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
