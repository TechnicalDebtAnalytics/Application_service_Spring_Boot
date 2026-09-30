package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.ClassRecommendationDTO;
import com.debtlens.backend.dto.response.ReportResponseDTO;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.repository.*;
import com.debtlens.backend.service.impl.ReportServiceImpl;
import com.debtlens.backend.security.RepositoryAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private Analysis_JobRepository analysisJobRepository;
    @Mock
    private Class_MetricsRepository classMetricsRepository;
    @Mock
    private Bug_PredictionRepository bugPredictionRepository;
    @Mock
    private SATD_DetectionRepository satdDetectionRepository;
    @Mock
    private Debt_ScoreRepository debtScoreRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private RepositoryAccessService repositoryAccessService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                classMetricsRepository,
                bugPredictionRepository,
                satdDetectionRepository,
                debtScoreRepository,
                reportRepository,
                repositoryAccessService
        );
    }

    @Test
    void generateReport_returnsPrioritizedRecommendationsOrderedByDebtScore() {
        Long analysisId = 100L;
        Analysis_Job job = new Analysis_Job();
        job.setAnalysisId(analysisId);

        Repository repo = new Repository();
        repo.setRepositoryId(10L);
        repo.setRepositoryName("DebtLens");
        repo.setDefaultBranch("main");
        job.setRepository(repo);

        when(repositoryAccessService.requireAnalysisReadAccess(analysisId)).thenReturn(job);
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report r = i.getArgument(0);
            r.setReportId(1L);
            return r;
        });

        // Class A: Low Debt
        Class_Metrics classA = new Class_Metrics();
        classA.setClassId(1L);
        classA.setClassName("OrderUtil");
        classA.setFilePath("src/OrderUtil.java");
        classA.setStartLine(1);
        classA.setEndLine(40);
        classA.setNumberOfLinesOfCode(40);
        classA.setCbo(2);
        classA.setLcom(2.0);

        // Class B: High Debt
        Class_Metrics classB = new Class_Metrics();
        classB.setClassId(2L);
        classB.setClassName("PaymentService");
        classB.setFilePath("src/PaymentService.java");
        classB.setStartLine(1);
        classB.setEndLine(200);
        classB.setNumberOfLinesOfCode(200);
        classB.setCbo(15);
        classB.setLcom(45.0);

        when(classMetricsRepository.findByAnalysisJobAnalysisIdOrderByFilePathAscStartLineAscClassNameAsc(analysisId))
                .thenReturn(List.of(classA, classB));

        Debt_Score dsA = new Debt_Score();
        dsA.setClassMetrics(classA);
        dsA.setTechnicalDebtScore(15.0);
        dsA.setHealthScore("EXCELLENT");
        dsA.setRiskLevel("LOW");

        Debt_Score dsB = new Debt_Score();
        dsB.setClassMetrics(classB);
        dsB.setTechnicalDebtScore(80.0);
        dsB.setHealthScore("POOR");
        dsB.setRiskLevel("CRITICAL");

        when(debtScoreRepository.findByClassMetricsAnalysisJobAnalysisId(analysisId))
                .thenReturn(List.of(dsA, dsB));

        Bug_Prediction bpB = new Bug_Prediction();
        bpB.setClassMetrics(classB);
        bpB.setProbabilityScore(0.85);

        when(bugPredictionRepository.findByClassMetricsAnalysisJobAnalysisId(analysisId))
                .thenReturn(List.of(bpB));

        ReportResponseDTO report = reportService.generateReport(analysisId);

        assertNotNull(report);
        assertEquals(2, report.getTotalClasses());
        assertEquals("DebtLens", report.getRepositoryName());

        List<ClassRecommendationDTO> list = report.getPrioritizedRefactoringList();
        assertEquals(2, list.size());
        // Highest debt class (PaymentService) should be rank 1 (refactor first!)
        assertEquals("PaymentService", list.get(0).getClassName());
        assertEquals(1, list.get(0).getRefactorPriorityRank());
        assertEquals(80.0, list.get(0).getTechnicalDebtScore());
        assertEquals("CRITICAL", list.get(0).getRiskLevel());
        assertFalse(list.get(0).getRecommendedActions().isEmpty());

        // Low debt class (OrderUtil) should be rank 2
        assertEquals("OrderUtil", list.get(1).getClassName());
        assertEquals(2, list.get(1).getRefactorPriorityRank());
    }

    @Test
    void generateReport_deniedAccessDoesNotPersistOrReadSensitiveData() {
        when(repositoryAccessService.requireAnalysisReadAccess(100L))
                .thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> reportService.generateReport(100L));

        verifyNoInteractions(reportRepository, classMetricsRepository, bugPredictionRepository,
                satdDetectionRepository, debtScoreRepository);
    }

    @Test
    void getReportHistory_authorizesBeforeReturningHistory() {
        Analysis_Job job = new Analysis_Job();
        job.setAnalysisId(100L);
        Report report = new Report();
        report.setReportId(1L);
        when(repositoryAccessService.requireAnalysisReadAccess(100L)).thenReturn(job);
        when(reportRepository.findByAnalysisJobAnalysisIdOrderByGeneratedAtDesc(100L))
                .thenReturn(List.of(report));

        List<Report> history = reportService.getReportHistory(100L);

        assertEquals(List.of(report), history);
    }
}
