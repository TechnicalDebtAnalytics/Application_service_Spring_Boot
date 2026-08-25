package com.debtlens.backend.entity;

import jakarta.persistence.*;

/**
 * Stores the complete metric vector for one analyzed class/file region within
 * an Analysis_Job.
 *
 * Class_Metrics references Analysis_Job directly because metrics are generated
 * analysis output, not report metadata. Future bug_predictions, debt_scores,
 * and class_comments records will reference this entity through class_id.
 */
@Entity
@Table(
        name = "class_metrics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_class_metrics_analysis_file_line_class",
                        columnNames = {
                                "analysis_job_id",
                                "file_path",
                                "start_line",
                                "class_name"
                        }
                )
        }
)
public class Class_Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    // Analysis job that produced this per-class metric vector.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_job_id", nullable = false)
    private Analysis_Job analysisJob;

    @Column(name = "class_name", nullable = false, length = 255)
    private String className;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    // Source-code location and size metrics
    @Column(name = "start_line", nullable = false)
    private Integer startLine;

    @Column(name = "end_line", nullable = false)
    private Integer endLine;

    @Column(name = "number_of_lines_of_code", nullable = false)
    private Integer numberOfLinesOfCode;

    // Object-oriented / structural metrics
    // Double preserves fractional analyzer values without imposing an
    // arbitrary fixed decimal scale on computed metrics.
    @Column(name = "dit", nullable = false)
    private Integer dit;

    @Column(name = "cbo", nullable = false)
    private Integer cbo;

    @Column(name = "fan_in", nullable = false)
    private Integer fanIn;

    @Column(name = "fan_out", nullable = false)
    private Integer fanOut;

    @Column(name = "lcom", nullable = false)
    private Double lcom;

    @Column(name = "noc", nullable = false)
    private Integer noc;

    @Column(name = "rfc", nullable = false)
    private Integer rfc;

    @Column(name = "wmc", nullable = false)
    private Double wmc;

    // Visibility/member-count metrics
    @Column(name = "number_of_attributes", nullable = false)
    private Integer numberOfAttributes;

    @Column(name = "number_of_methods", nullable = false)
    private Integer numberOfMethods;

    @Column(name = "number_of_private_attributes", nullable = false)
    private Integer numberOfPrivateAttributes;

    @Column(name = "number_of_private_methods", nullable = false)
    private Integer numberOfPrivateMethods;

    @Column(name = "number_of_public_attributes", nullable = false)
    private Integer numberOfPublicAttributes;

    @Column(name = "number_of_public_methods", nullable = false)
    private Integer numberOfPublicMethods;

    // Git history and churn metrics
    @Column(name = "number_of_versions_until", nullable = false)
    private Integer numberOfVersionsUntil;

    @Column(name = "number_of_authors_until", nullable = false)
    private Integer numberOfAuthorsUntil;

    @Column(name = "lines_added_until", nullable = false)
    private Integer linesAddedUntil;

    @Column(name = "max_lines_added_until", nullable = false)
    private Integer maxLinesAddedUntil;

    @Column(name = "avg_lines_added_until", nullable = false)
    private Double avgLinesAddedUntil;

    @Column(name = "lines_removed_until", nullable = false)
    private Integer linesRemovedUntil;

    @Column(name = "max_lines_removed_until", nullable = false)
    private Integer maxLinesRemovedUntil;

    @Column(name = "avg_lines_removed_until", nullable = false)
    private Double avgLinesRemovedUntil;

    @Column(name = "code_churn_until", nullable = false)
    private Integer codeChurnUntil;

    @Column(name = "max_code_churn_until", nullable = false)
    private Integer maxCodeChurnUntil;

    @Column(name = "avg_code_churn_until", nullable = false)
    private Double avgCodeChurnUntil;

    @Column(name = "age_with_respect_to", nullable = false)
    private Double ageWithRespectTo;

    @Column(name = "weighted_age_with_respect_to", nullable = false)
    private Double weightedAgeWithRespectTo;

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Analysis_Job getAnalysisJob() {
        return analysisJob;
    }

    public void setAnalysisJob(Analysis_Job analysisJob) {
        this.analysisJob = analysisJob;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Integer getNumberOfLinesOfCode() {
        return numberOfLinesOfCode;
    }

    public void setNumberOfLinesOfCode(Integer numberOfLinesOfCode) {
        this.numberOfLinesOfCode = numberOfLinesOfCode;
    }

    public Integer getDit() {
        return dit;
    }

    public void setDit(Integer dit) {
        this.dit = dit;
    }

    public Integer getCbo() {
        return cbo;
    }

    public void setCbo(Integer cbo) {
        this.cbo = cbo;
    }

    public Integer getFanIn() {
        return fanIn;
    }

    public void setFanIn(Integer fanIn) {
        this.fanIn = fanIn;
    }

    public Integer getFanOut() {
        return fanOut;
    }

    public void setFanOut(Integer fanOut) {
        this.fanOut = fanOut;
    }

    public Double getLcom() {
        return lcom;
    }

    public void setLcom(Double lcom) {
        this.lcom = lcom;
    }

    public Integer getNoc() {
        return noc;
    }

    public void setNoc(Integer noc) {
        this.noc = noc;
    }

    public Integer getRfc() {
        return rfc;
    }

    public void setRfc(Integer rfc) {
        this.rfc = rfc;
    }

    public Double getWmc() {
        return wmc;
    }

    public void setWmc(Double wmc) {
        this.wmc = wmc;
    }

    public Integer getNumberOfAttributes() {
        return numberOfAttributes;
    }

    public void setNumberOfAttributes(Integer numberOfAttributes) {
        this.numberOfAttributes = numberOfAttributes;
    }

    public Integer getNumberOfMethods() {
        return numberOfMethods;
    }

    public void setNumberOfMethods(Integer numberOfMethods) {
        this.numberOfMethods = numberOfMethods;
    }

    public Integer getNumberOfPrivateAttributes() {
        return numberOfPrivateAttributes;
    }

    public void setNumberOfPrivateAttributes(Integer numberOfPrivateAttributes) {
        this.numberOfPrivateAttributes = numberOfPrivateAttributes;
    }

    public Integer getNumberOfPrivateMethods() {
        return numberOfPrivateMethods;
    }

    public void setNumberOfPrivateMethods(Integer numberOfPrivateMethods) {
        this.numberOfPrivateMethods = numberOfPrivateMethods;
    }

    public Integer getNumberOfPublicAttributes() {
        return numberOfPublicAttributes;
    }

    public void setNumberOfPublicAttributes(Integer numberOfPublicAttributes) {
        this.numberOfPublicAttributes = numberOfPublicAttributes;
    }

    public Integer getNumberOfPublicMethods() {
        return numberOfPublicMethods;
    }

    public void setNumberOfPublicMethods(Integer numberOfPublicMethods) {
        this.numberOfPublicMethods = numberOfPublicMethods;
    }

    public Integer getNumberOfVersionsUntil() {
        return numberOfVersionsUntil;
    }

    public void setNumberOfVersionsUntil(Integer numberOfVersionsUntil) {
        this.numberOfVersionsUntil = numberOfVersionsUntil;
    }

    public Integer getNumberOfAuthorsUntil() {
        return numberOfAuthorsUntil;
    }

    public void setNumberOfAuthorsUntil(Integer numberOfAuthorsUntil) {
        this.numberOfAuthorsUntil = numberOfAuthorsUntil;
    }

    public Integer getLinesAddedUntil() {
        return linesAddedUntil;
    }

    public void setLinesAddedUntil(Integer linesAddedUntil) {
        this.linesAddedUntil = linesAddedUntil;
    }

    public Integer getMaxLinesAddedUntil() {
        return maxLinesAddedUntil;
    }

    public void setMaxLinesAddedUntil(Integer maxLinesAddedUntil) {
        this.maxLinesAddedUntil = maxLinesAddedUntil;
    }

    public Double getAvgLinesAddedUntil() {
        return avgLinesAddedUntil;
    }

    public void setAvgLinesAddedUntil(Double avgLinesAddedUntil) {
        this.avgLinesAddedUntil = avgLinesAddedUntil;
    }

    public Integer getLinesRemovedUntil() {
        return linesRemovedUntil;
    }

    public void setLinesRemovedUntil(Integer linesRemovedUntil) {
        this.linesRemovedUntil = linesRemovedUntil;
    }

    public Integer getMaxLinesRemovedUntil() {
        return maxLinesRemovedUntil;
    }

    public void setMaxLinesRemovedUntil(Integer maxLinesRemovedUntil) {
        this.maxLinesRemovedUntil = maxLinesRemovedUntil;
    }

    public Double getAvgLinesRemovedUntil() {
        return avgLinesRemovedUntil;
    }

    public void setAvgLinesRemovedUntil(Double avgLinesRemovedUntil) {
        this.avgLinesRemovedUntil = avgLinesRemovedUntil;
    }

    public Integer getCodeChurnUntil() {
        return codeChurnUntil;
    }

    public void setCodeChurnUntil(Integer codeChurnUntil) {
        this.codeChurnUntil = codeChurnUntil;
    }

    public Integer getMaxCodeChurnUntil() {
        return maxCodeChurnUntil;
    }

    public void setMaxCodeChurnUntil(Integer maxCodeChurnUntil) {
        this.maxCodeChurnUntil = maxCodeChurnUntil;
    }

    public Double getAvgCodeChurnUntil() {
        return avgCodeChurnUntil;
    }

    public void setAvgCodeChurnUntil(Double avgCodeChurnUntil) {
        this.avgCodeChurnUntil = avgCodeChurnUntil;
    }

    public Double getAgeWithRespectTo() {
        return ageWithRespectTo;
    }

    public void setAgeWithRespectTo(Double ageWithRespectTo) {
        this.ageWithRespectTo = ageWithRespectTo;
    }

    public Double getWeightedAgeWithRespectTo() {
        return weightedAgeWithRespectTo;
    }

    public void setWeightedAgeWithRespectTo(Double weightedAgeWithRespectTo) {
        this.weightedAgeWithRespectTo = weightedAgeWithRespectTo;
    }
}
