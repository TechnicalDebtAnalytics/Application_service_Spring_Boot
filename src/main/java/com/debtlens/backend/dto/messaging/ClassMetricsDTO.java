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
public class ClassMetricsDTO {
    private String className;
    private String filePath;

    private int startLine;
    private int endLine;

    private int dit;
    private int cbo;
    private int fanin;
    private int fanout;
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
    private java.util.List<String> comments = new java.util.ArrayList<>();
}
