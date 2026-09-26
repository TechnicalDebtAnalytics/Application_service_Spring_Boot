package com.debtlens.backend.integration.rabbitmq;

import com.debtlens.backend.config.RabbitMQConfig;
import com.debtlens.backend.dto.messaging.AnalysisJobMessage;
import com.debtlens.backend.dto.messaging.AnalysisResultDTO;
import com.debtlens.backend.dto.messaging.MLBugPredictionDTO;
import com.debtlens.backend.dto.messaging.MLClassResultDTO;
import com.debtlens.backend.dto.messaging.MLJobMessage;
import com.debtlens.backend.dto.messaging.MLResultMessage;
import com.debtlens.backend.dto.messaging.MLSatdDetectionDTO;
import com.debtlens.backend.engine.DebtAssessment;
import com.debtlens.backend.engine.DebtScoreEngine;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.Analysis_Status_History;
import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Bug_Prediction;
import com.debtlens.backend.entity.Class_Comment;
import com.debtlens.backend.entity.Class_Metrics;
import com.debtlens.backend.entity.Debt_Score;
import com.debtlens.backend.entity.SATD_Detection;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Analysis_Status_HistoryRepository;
import com.debtlens.backend.repository.Bug_PredictionRepository;
import com.debtlens.backend.repository.Class_CommentRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.Debt_ScoreRepository;
import com.debtlens.backend.repository.SATD_DetectionRepository;
import com.debtlens.backend.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RabbitMQMessagingTest {

    private RabbitTemplate rabbitTemplate;
    private Analysis_JobRepository analysisJobRepository;
    private Analysis_Status_HistoryRepository statusHistoryRepository;
    private Class_MetricsRepository classMetricsRepository;
    private Class_CommentRepository classCommentRepository;
    private Bug_PredictionRepository bugPredictionRepository;
    private SATD_DetectionRepository satdDetectionRepository;
    private Debt_ScoreRepository debtScoreRepository;
    private DebtScoreEngine debtScoreEngine;
    private MLResultConsumer mlResultConsumer;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        analysisJobRepository = mock(Analysis_JobRepository.class);
        statusHistoryRepository = mock(Analysis_Status_HistoryRepository.class);
        classMetricsRepository = mock(Class_MetricsRepository.class);
        classCommentRepository = mock(Class_CommentRepository.class);
        bugPredictionRepository = mock(Bug_PredictionRepository.class);
        satdDetectionRepository = mock(SATD_DetectionRepository.class);
        debtScoreRepository = mock(Debt_ScoreRepository.class);
        debtScoreEngine = mock(DebtScoreEngine.class);
        mlResultConsumer = new MLResultConsumer(
                analysisJobRepository,
                statusHistoryRepository,
                classMetricsRepository,
                classCommentRepository,
                bugPredictionRepository,
                satdDetectionRepository,
                debtScoreRepository,
                debtScoreEngine
        );
    }

    @Test
    void analysisJobProducer_shouldPublishMessageToConfiguredQueue() {
        AnalysisJobMessage message = AnalysisJobMessage.builder().jobId("100").repositoryId("42").build();

        new AnalysisJobProducer(rabbitTemplate).publishAnalysisJob(message);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.ANALYSIS_JOB_QUEUE, message);
    }

    @Test
    void analysisJobProducer_shouldPropagatePublishingFailure() {
        AnalysisJobMessage message = AnalysisJobMessage.builder().jobId("100").build();
        doThrow(new AmqpException("publish failed"))
                .when(rabbitTemplate).convertAndSend(RabbitMQConfig.ANALYSIS_JOB_QUEUE, message);

        AmqpException exception = assertThrows(
                AmqpException.class,
                () -> new AnalysisJobProducer(rabbitTemplate).publishAnalysisJob(message)
        );

        assertEquals("publish failed", exception.getMessage());
    }

    @Test
    void mlJobProducer_shouldPublishMessageToConfiguredQueue() {
        MLJobMessage message = MLJobMessage.builder().jobId("100").classes(List.of()).build();

        new MLJobProducer(rabbitTemplate).publishMLJob(message);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.ML_JOB_CREATION_QUEUE, message);
    }

    @Test
    void mlJobProducer_shouldPropagatePublishingFailure() {
        MLJobMessage message = MLJobMessage.builder().jobId("100").build();
        doThrow(new AmqpException("ML publish failed"))
                .when(rabbitTemplate).convertAndSend(RabbitMQConfig.ML_JOB_CREATION_QUEUE, message);

        AmqpException exception = assertThrows(
                AmqpException.class,
                () -> new MLJobProducer(rabbitTemplate).publishMLJob(message)
        );

        assertEquals("ML publish failed", exception.getMessage());
    }

    @Test
    void analysisResultConsumer_shouldDelegateValidResult() {
        AnalysisService analysisService = mock(AnalysisService.class);
        AnalysisResultDTO result = AnalysisResultDTO.builder().jobId("100").status("SUCCESS").build();

        new AnalysisResultConsumer(analysisService).consumeAnalysisResult(result);

        verify(analysisService).processAnalysisResult(result);
    }

    @Test
    void analysisResultConsumer_shouldContainDownstreamServiceFailure() {
        AnalysisService analysisService = mock(AnalysisService.class);
        AnalysisResultDTO result = AnalysisResultDTO.builder().jobId("100").status("SUCCESS").build();
        doThrow(new RuntimeException("database unavailable")).when(analysisService).processAnalysisResult(result);

        assertDoesNotThrow(() -> new AnalysisResultConsumer(analysisService).consumeAnalysisResult(result));
        verify(analysisService).processAnalysisResult(result);
    }

    @Test
    void mlResultConsumer_shouldPersistPredictionsDebtScoreAndCompleteJob() {
        Analysis_Job job = job(100L);
        Class_Metrics classMetric = new Class_Metrics();
        classMetric.setClassId(500L);
        Class_Comment comment = new Class_Comment();
        comment.setCommentId(600L);
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(classMetricsRepository.findById(500L)).thenReturn(Optional.of(classMetric));
        when(classCommentRepository.findById(600L)).thenReturn(Optional.of(comment));
        when(bugPredictionRepository.findByClassMetricsClassId(500L)).thenReturn(Optional.empty());
        when(satdDetectionRepository.findByClassCommentCommentId(600L)).thenReturn(Optional.empty());
        when(debtScoreRepository.findByClassMetricsClassId(500L)).thenReturn(Optional.empty());
        when(debtScoreEngine.calculateDebtScore(any(), anyDouble(), anyList()))
                .thenReturn(DebtAssessment.builder()
                        .technicalDebtScore(72.0)
                        .healthScore("POOR")
                        .riskLevel("HIGH")
                        .build());
        MLResultMessage message = resultMessage(
                MLBugPredictionDTO.builder().probabilityScore(1.5).isDefective(true).build(),
                MLSatdDetectionDTO.builder()
                        .commentId(600L)
                        .category(null)
                        .confidenceScore(-0.4)
                        .isDebt(true)
                        .build()
        );

        mlResultConsumer.processMLResult(message);

        ArgumentCaptor<Bug_Prediction> bugCaptor = ArgumentCaptor.forClass(Bug_Prediction.class);
        verify(bugPredictionRepository).save(bugCaptor.capture());
        assertEquals(classMetric, bugCaptor.getValue().getClassMetrics());
        assertEquals(1.0, bugCaptor.getValue().getProbabilityScore());

        ArgumentCaptor<SATD_Detection> satdCaptor = ArgumentCaptor.forClass(SATD_Detection.class);
        verify(satdDetectionRepository).save(satdCaptor.capture());
        assertEquals(comment, satdCaptor.getValue().getClassComment());
        assertEquals("WITHOUT_CLASSIFICATION", satdCaptor.getValue().getCategory());
        assertEquals(0.0, satdCaptor.getValue().getConfidenceScore());

        ArgumentCaptor<Debt_Score> debtCaptor = ArgumentCaptor.forClass(Debt_Score.class);
        verify(debtScoreRepository).save(debtCaptor.capture());
        assertEquals(72.0, debtCaptor.getValue().getTechnicalDebtScore());
        assertEquals("POOR", debtCaptor.getValue().getHealthScore());
        assertEquals("HIGH", debtCaptor.getValue().getRiskLevel());

        assertEquals(AnalysisJobStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(analysisJobRepository).save(job);
        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(AnalysisJobStatus.COMPLETED, historyCaptor.getValue().getStatus());
        assertEquals(
                "Analysis and ML pipeline completed: Persisted 1 bug predictions, 1 SATD classifications, and 1 debt scores.",
                historyCaptor.getValue().getMessage()
        );
    }

    @Test
    void mlResultConsumer_shouldIgnoreMissingMalformedAndUnknownJobIds() {
        mlResultConsumer.processMLResult(MLResultMessage.builder().jobId(null).build());
        mlResultConsumer.processMLResult(MLResultMessage.builder().jobId("  ").build());
        mlResultConsumer.processMLResult(MLResultMessage.builder().jobId("not-a-number").build());
        when(analysisJobRepository.findById(404L)).thenReturn(Optional.empty());
        mlResultConsumer.processMLResult(MLResultMessage.builder().jobId("404").build());

        verify(analysisJobRepository).findById(404L);
        verify(analysisJobRepository, never()).save(any());
        verifyNoInteractions(statusHistoryRepository, classMetricsRepository, classCommentRepository,
                bugPredictionRepository, satdDetectionRepository, debtScoreRepository, debtScoreEngine);
    }

    @Test
    void mlResultConsumer_shouldIgnoreResultWithoutClassPredictions() {
        Analysis_Job job = job(100L);
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));

        mlResultConsumer.processMLResult(MLResultMessage.builder().jobId("100").classes(List.of()).build());

        assertEquals(AnalysisJobStatus.RUNNING, job.getStatus());
        verify(analysisJobRepository, never()).save(any());
        verifyNoInteractions(statusHistoryRepository, classMetricsRepository, classCommentRepository,
                bugPredictionRepository, satdDetectionRepository, debtScoreRepository, debtScoreEngine);
    }

    @Test
    void mlResultConsumer_shouldSkipUnknownClassAndStillCompleteJob() {
        Analysis_Job job = job(100L);
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(classMetricsRepository.findById(500L)).thenReturn(Optional.empty());
        MLResultMessage message = MLResultMessage.builder()
                .jobId("100")
                .classes(List.of(MLClassResultDTO.builder().classId(500L).build()))
                .build();

        mlResultConsumer.processMLResult(message);

        assertEquals(AnalysisJobStatus.COMPLETED, job.getStatus());
        verify(analysisJobRepository).save(job);
        verify(statusHistoryRepository).save(any(Analysis_Status_History.class));
        verifyNoInteractions(bugPredictionRepository, satdDetectionRepository, debtScoreRepository, debtScoreEngine);
    }

    @Test
    void mlResultConsumer_shouldPropagateDebtCalculationFailureWithoutCompletingJob() {
        Analysis_Job job = job(100L);
        Class_Metrics classMetric = new Class_Metrics();
        classMetric.setClassId(500L);
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(classMetricsRepository.findById(500L)).thenReturn(Optional.of(classMetric));
        when(debtScoreEngine.calculateDebtScore(any(), anyDouble(), anyList()))
                .thenThrow(new RuntimeException("scoring failed"));
        MLResultMessage message = resultMessage((MLBugPredictionDTO) null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mlResultConsumer.processMLResult(message)
        );

        assertEquals("scoring failed", exception.getMessage());
        assertEquals(AnalysisJobStatus.RUNNING, job.getStatus());
        verify(analysisJobRepository, never()).save(any());
        verifyNoInteractions(statusHistoryRepository, debtScoreRepository);
    }

    private static Analysis_Job job(Long id) {
        Analysis_Job job = new Analysis_Job();
        job.setAnalysisId(id);
        job.setStatus(AnalysisJobStatus.RUNNING);
        return job;
    }

    private static MLResultMessage resultMessage(MLSatdDetectionDTO... detections) {
        return resultMessage(null, detections);
    }

    private static MLResultMessage resultMessage(
            MLBugPredictionDTO bugPrediction,
            MLSatdDetectionDTO... detections
    ) {
        MLClassResultDTO classResult = MLClassResultDTO.builder()
                .classId(500L)
                .bugPrediction(bugPrediction)
                .satdDetections(List.of(detections))
                .build();
        return MLResultMessage.builder()
                .jobId("100")
                .status("SUCCESS")
                .classes(List.of(classResult))
                .build();
    }
}
