package com.debtlens.backend.dto.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLSatdDetectionDTO {
    private Long commentId;
    private String comment;
    private String category;
    private double confidenceScore;
    private boolean isDebt;
}
