package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.ClassRecommendationDTO;
import com.debtlens.backend.dto.response.RefactoringActionDTO;
import com.debtlens.backend.dto.response.ReportResponseDTO;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.*;
import com.debtlens.backend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final Analysis_JobRepository analysisJobRepository;
    private final Class_MetricsRepository classMetricsRepository;
    private final Bug_PredictionRepository bugPredictionRepository;
    private final SATD_DetectionRepository satdDetectionRepository;
    private final Debt_ScoreRepository debtScoreRepository;
    private final ReportRepository reportRepository;

    public ReportServiceImpl(
            Analysis_JobRepository analysisJobRepository,
            Class_MetricsRepository classMetricsRepository,
            Bug_PredictionRepository bugPredictionRepository,
            SATD_DetectionRepository satdDetectionRepository,
            Debt_ScoreRepository debtScoreRepository,
            ReportRepository reportRepository
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.classMetricsRepository = classMetricsRepository;
        this.bugPredictionRepository = bugPredictionRepository;
        this.satdDetectionRepository = satdDetectionRepository;
        this.debtScoreRepository = debtScoreRepository;
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional
    public ReportResponseDTO generateReport(Long analysisId) {
        Analysis_Job job = getAnalysisJob(analysisId);

        // 1. Audit generation in the reports table
        Report reportLog = new Report();
        reportLog.setAnalysisJob(job);
        reportLog.setGeneratedAt(LocalDateTime.now());
        Report savedReport = reportRepository.save(reportLog);

        // 2. Fetch ground-truth facts from PostgreSQL
        List<ClassRecommendationDTO> prioritizedList = buildPrioritizedRecommendations(analysisId);

        // 3. Compute overall repository summaries
        int totalClasses = prioritizedList.size();
        int defectiveCount = (int) prioritizedList.stream()
                .filter(c -> c.getBugProbability() != null && c.getBugProbability() >= 0.50)
                .count();

        double avgDebtScore = totalClasses > 0
                ? prioritizedList.stream()
                .mapToDouble(c -> c.getTechnicalDebtScore() != null ? c.getTechnicalDebtScore() : 0.0)
                .average()
                .orElse(0.0)
                : 0.0;
        avgDebtScore = Math.round(avgDebtScore * 10.0) / 10.0;

        String overallHealth;
        if (avgDebtScore < 25.0) {
            overallHealth = "EXCELLENT";
        } else if (avgDebtScore < 50.0) {
            overallHealth = "GOOD";
        } else if (avgDebtScore < 75.0) {
            overallHealth = "FAIR";
        } else {
            overallHealth = "POOR";
        }

        String overallRisk;
        if (avgDebtScore >= 75.0 || defectiveCount > (totalClasses * 0.4)) {
            overallRisk = "CRITICAL";
        } else if (avgDebtScore >= 50.0 || defectiveCount > 0) {
            overallRisk = "HIGH";
        } else if (avgDebtScore >= 25.0) {
            overallRisk = "MEDIUM";
        } else {
            overallRisk = "LOW";
        }

        int totalSatdComments = (int) satdDetectionRepository
                .findByClassCommentClassMetricsAnalysisJobAnalysisId(analysisId)
                .stream()
                .filter(s -> !s.getCategory().equalsIgnoreCase("non_debt"))
                .count();

        String repoName = job.getRepository() != null ? job.getRepository().getRepositoryName() : "Unknown Repository";
        Long repoId = job.getRepository() != null ? job.getRepository().getRepositoryId() : null;
        String branch = job.getRepository() != null ? job.getRepository().getDefaultBranch() : "main";

        return ReportResponseDTO.builder()
                .reportId(savedReport.getReportId())
                .analysisId(analysisId)
                .repositoryId(repoId)
                .repositoryName(repoName)
                .branch(branch)
                .generatedAt(savedReport.getGeneratedAt())
                .overallDebtScore(avgDebtScore)
                .overallHealthScore(overallHealth)
                .overallRiskLevel(overallRisk)
                .totalClasses(totalClasses)
                .defectiveClassesCount(defectiveCount)
                .totalSatdComments(totalSatdComments)
                .prioritizedRefactoringList(prioritizedList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRecommendationDTO> getPrioritizedRecommendations(Long analysisId) {
        getAnalysisJob(analysisId);
        return buildPrioritizedRecommendations(analysisId);
    }

    private List<ClassRecommendationDTO> buildPrioritizedRecommendations(Long analysisId) {
        List<Class_Metrics> classMetricsList = classMetricsRepository
                .findByAnalysisJobAnalysisIdOrderByFilePathAscStartLineAscClassNameAsc(analysisId);

        if (classMetricsList.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch pre-computed assessments and predictions
        Map<Long, Debt_Score> debtScoreMap = debtScoreRepository
                .findByClassMetricsAnalysisJobAnalysisId(analysisId)
                .stream()
                .filter(ds -> ds.getClassMetrics() != null && ds.getClassMetrics().getClassId() != null)
                .collect(Collectors.toMap(ds -> ds.getClassMetrics().getClassId(), ds -> ds, (a, b) -> a));

        Map<Long, Bug_Prediction> bugMap = bugPredictionRepository
                .findByClassMetricsAnalysisJobAnalysisId(analysisId)
                .stream()
                .filter(bp -> bp.getClassMetrics() != null && bp.getClassMetrics().getClassId() != null)
                .collect(Collectors.toMap(bp -> bp.getClassMetrics().getClassId(), bp -> bp, (a, b) -> a));

        List<SATD_Detection> allSatd = satdDetectionRepository
                .findByClassCommentClassMetricsAnalysisJobAnalysisId(analysisId);

        Map<Long, List<SATD_Detection>> classSatdMap = new HashMap<>();
        for (SATD_Detection sd : allSatd) {
            if (sd.getClassComment() != null
                    && sd.getClassComment().getClassMetrics() != null
                    && sd.getClassComment().getClassMetrics().getClassId() != null) {
                Long classId = sd.getClassComment().getClassMetrics().getClassId();
                classSatdMap.computeIfAbsent(classId, k -> new ArrayList<>()).add(sd);
            }
        }

        List<ClassRecommendationDTO> recommendations = new ArrayList<>();

        for (Class_Metrics metrics : classMetricsList) {
            Long classId = metrics.getClassId();
            Debt_Score debtScore = debtScoreMap.get(classId);
            Bug_Prediction bugPrediction = bugMap.get(classId);
            List<SATD_Detection> satdList = classSatdMap.getOrDefault(classId, Collections.emptyList());

            double score = debtScore != null ? debtScore.getTechnicalDebtScore() : 0.0;
            String health = debtScore != null ? debtScore.getHealthScore() : "EXCELLENT";
            String risk = debtScore != null ? debtScore.getRiskLevel() : "LOW";
            double bugProb = bugPrediction != null ? bugPrediction.getProbabilityScore() : 0.0;

            List<String> drivers = new ArrayList<>();
            List<RefactoringActionDTO> actions = new ArrayList<>();

            // 1. Defect Risk Action
            if (bugProb >= 0.50) {
                drivers.add("High Defect Risk (" + Math.round(bugProb * 100) + "% defect probability)");
                actions.add(RefactoringActionDTO.builder()
                        .type("DEFECT_PREVENTION")
                        .priority("CRITICAL")
                        .title("Write Targeted Unit & Regression Tests")
                        .description("ML defect prediction indicates this class has a high probability of containing bugs.")
                        .suggestedRefactoring("Increase test coverage, add boundary/edge-case tests, and review recent churn before release.")
                        .build());
            }

            // 2. High Coupling Action (CBO)
            if (metrics.getCbo() != null && metrics.getCbo() > 10) {
                drivers.add("High Coupling (CBO = " + metrics.getCbo() + " dependencies)");
                actions.add(RefactoringActionDTO.builder()
                        .type("DECOUPLING")
                        .priority("HIGH")
                        .title("Decouple External Dependencies")
                        .description("Class is coupled to " + metrics.getCbo() + " other classes, making changes fragile.")
                        .suggestedRefactoring("Apply Dependency Inversion or introduce Facade/Mediator patterns to isolate dependencies.")
                        .build());
            }

            // 3. Low Cohesion Action (LCOM)
            if (metrics.getLcom() != null && metrics.getLcom() > 30.0) {
                drivers.add("Low Cohesion (LCOM = " + metrics.getLcom() + ")");
                actions.add(RefactoringActionDTO.builder()
                        .type("COHESION_IMPROVEMENT")
                        .priority("HIGH")
                        .title("Split Class by Responsibility")
                        .description("Methods in this class operate on disjoint field subsets, violating Single Responsibility.")
                        .suggestedRefactoring("Extract cohesive method groups into separate focused classes or delegate services.")
                        .build());
            }

            // 4. God Class / Complexity Action (LOC & WMC)
            if ((metrics.getNumberOfLinesOfCode() != null && metrics.getNumberOfLinesOfCode() > 400)
                    || (metrics.getWmc() != null && metrics.getWmc() > 35.0)) {
                drivers.add("High Complexity (LOC = " + metrics.getNumberOfLinesOfCode() + ", WMC = " + metrics.getWmc() + ")");
                actions.add(RefactoringActionDTO.builder()
                        .type("COMPLEXITY_REDUCTION")
                        .priority("MEDIUM")
                        .title("Reduce Class Size and Cognitive Complexity")
                        .description("Class has high cyclomatic complexity and large line count.")
                        .suggestedRefactoring("Extract helper methods and delegate sub-tasks into utility or strategy components.")
                        .build());
            }

            // 5. SATD Comment Actions
            List<SATD_Detection> debtSatds = satdList.stream()
                    .filter(s -> !s.getCategory().equalsIgnoreCase("non_debt"))
                    .collect(Collectors.toList());

            if (!debtSatds.isEmpty()) {
                drivers.add(debtSatds.size() + " Self-Admitted Debt Comments detected");
                for (SATD_Detection sd : debtSatds) {
                    String commentText = sd.getClassComment() != null ? sd.getClassComment().getComment() : "";
                    String category = sd.getCategory() != null ? sd.getCategory() : "Technical Debt";
                    actions.add(RefactoringActionDTO.builder()
                            .type("SATD_RESOLUTION")
                            .priority(category.toLowerCase().contains("defect") ? "HIGH" : "MEDIUM")
                            .title("Resolve Admitted " + category)
                            .description("Developer comment explicitly notes technical debt: \"" + commentText + "\"")
                            .suggestedRefactoring("Refactor or complete the TODO/FIXME task to eliminate known codebase debt.")
                            .build());
                }
            }

            // 6. High Churn & Author Volatility
            if (metrics.getCodeChurnUntil() != null && metrics.getCodeChurnUntil() > 500
                    && metrics.getNumberOfAuthorsUntil() != null && metrics.getNumberOfAuthorsUntil() > 4) {
                drivers.add("High Churn (" + metrics.getCodeChurnUntil() + " lines churned across " + metrics.getNumberOfAuthorsUntil() + " authors)");
                actions.add(RefactoringActionDTO.builder()
                        .type("VOLATILITY_STABILIZATION")
                        .priority("MEDIUM")
                        .title("Establish Modular API Boundary")
                        .description("Frequently modified by multiple authors, creating potential merge and regression risks.")
                        .suggestedRefactoring("Lock down interfaces and establish clear module boundaries to reduce cross-team churn.")
                        .build());
            }

            if (drivers.isEmpty()) {
                drivers.add("Clean architecture (No major debt flags detected)");
            }

            ClassRecommendationDTO rec = ClassRecommendationDTO.builder()
                    .classId(classId)
                    .className(metrics.getClassName())
                    .filePath(metrics.getFilePath())
                    .startLine(metrics.getStartLine())
                    .endLine(metrics.getEndLine())
                    .numberOfLinesOfCode(metrics.getNumberOfLinesOfCode())
                    .technicalDebtScore(score)
                    .healthScore(health)
                    .riskLevel(risk)
                    .bugProbability(bugProb)
                    .primaryDrivers(drivers)
                    .recommendedActions(actions)
                    .build();

            recommendations.add(rec);
        }

        // Sort descending by technical debt score (highest debt first to refactor)
        recommendations.sort(Comparator
                .comparing((ClassRecommendationDTO c) -> c.getTechnicalDebtScore() != null ? c.getTechnicalDebtScore() : 0.0)
                .reversed()
                .thenComparing((ClassRecommendationDTO c) -> c.getBugProbability() != null ? c.getBugProbability() : 0.0, Comparator.reverseOrder())
        );

        // Assign 1-indexed refactoring priority rank
        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRefactorPriorityRank(i + 1);
        }

        return recommendations;
    }

    private Analysis_Job getAnalysisJob(Long analysisId) {
        return analysisJobRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job #" + analysisId + " not found"));
    }
}