package com.debtlens.backend.dto.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLClassResultDTO {

    private Long classId;
    private String className;
    private String filePath;
    private int startLine;
    private int endLine;

    private MLBugPredictionDTO bugPrediction;

    @Builder.Default
    private List<MLSatdDetectionDTO> satdDetections = new ArrayList<>();
}
