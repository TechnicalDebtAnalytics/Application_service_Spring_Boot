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
public class MLClassMetricDTO {

    private Long classId;
    private String className;
    private String filePath;

    private int startLine;
    private int endLine;

    // 28 Canonical Bug Prediction Metrics
    private int cbo;
    private int dit;
    private int fanIn;
    private int fanOut;
    private double lcom;
    private int noc;
    private int numberOfAttributes;
    private int numberOfLinesOfCode;
    private int numberOfMethods;
    private int numberOfPrivateAttributes;
    private int numberOfPrivateMethods;
    private int numberOfPublicAttributes;
    private int numberOfPublicMethods;
    private int rfc;
    private double wmc;

    // Git history metrics
    private int numberOfVersionsUntil;
    private int numberOfAuthorsUntil;
    private int linesAddedUntil;
    private int maxLinesAddedUntil;
    private double avgLinesAddedUntil;
    private int linesRemovedUntil;
    private int maxLinesRemovedUntil;
    private double avgLinesRemovedUntil;
    private int codeChurnUntil;
    private int maxCodeChurnUntil;
    private double avgCodeChurnUntil;
    private double ageWithRespectTo;
    private double weightedAgeWithRespectTo;

    @Builder.Default
    private List<MLCommentDTO> comments = new ArrayList<>();
}
