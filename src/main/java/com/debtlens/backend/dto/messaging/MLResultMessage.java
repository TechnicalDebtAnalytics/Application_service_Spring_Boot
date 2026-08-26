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
public class MLResultMessage {

    private String jobId;
    private String repositoryId;
    private String status;
    private int totalClassesAnalyzed;
    private int defectiveClassesCount;
    private int totalCommentsClassified;
    private int totalSatdCount;

    @Builder.Default
    private List<MLClassResultDTO> classes = new ArrayList<>();

    private String timestamp;
}
