package com.debtlens.backend.integration.rabbitmq;

import com.debtlens.backend.config.RabbitMQConfig;
import com.debtlens.backend.dto.messaging.MLBugPredictionDTO;
import com.debtlens.backend.dto.messaging.MLClassResultDTO;
import com.debtlens.backend.dto.messaging.MLResultMessage;
import com.debtlens.backend.dto.messaging.MLSatdDetectionDTO;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MLResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(MLResultConsumer.class);

    private final Analysis_JobRepository analysisJobRepository;
    private final Analysis_Status_HistoryRepository statusHistoryRepository;
    private final Class_MetricsRepository classMetricsRepository;
    private final Class_CommentRepository classCommentRepository;
    private final Bug_PredictionRepository bugPredictionRepository;
    private final SATD_DetectionRepository satdDetectionRepository;
    private final Debt_ScoreRepository debtScoreRepository;
    private final com.debtlens.backend.engine.DebtScoreEngine debtScoreEngine;

    public MLResultConsumer(
            Analysis_JobRepository analysisJobRepository,
            Analysis_Status_HistoryRepository statusHistoryRepository,
            Class_MetricsRepository classMetricsRepository,
            Class_CommentRepository classCommentRepository,
            Bug_PredictionRepository bugPredictionRepository,
            SATD_DetectionRepository satdDetectionRepository,
            Debt_ScoreRepository debtScoreRepository,
            com.debtlens.backend.engine.DebtScoreEngine debtScoreEngine
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.classMetricsRepository = classMetricsRepository;
        this.classCommentRepository = classCommentRepository;
        this.bugPredictionRepository = bugPredictionRepository;
        this.satdDetectionRepository = satdDetectionRepository;
        this.debtScoreRepository = debtScoreRepository;
        this.debtScoreEngine = debtScoreEngine;
    }

    @RabbitListener(queues = RabbitMQConfig.ML_JOB_RESULTS_QUEUE)
    @Transactional
    public void processMLResult(MLResultMessage result) {
        log.info("Received ML prediction result message for jobId: {}, status: {}", result.getJobId(), result.getStatus());

        if (result.getJobId() == null || result.getJobId().isBlank()) {
            log.error("Received ML result without valid jobId");
            return;
        }

        Long analysisId;
        try {
            analysisId = Long.parseLong(result.getJobId());
        } catch (NumberFormatException e) {
            log.error("Invalid numeric jobId in ML result: {}", result.getJobId());
            return;
        }

        Analysis_Job job = analysisJobRepository.findById(analysisId).orElse(null);
        if (job == null) {
            log.error("Analysis job #{} not found for ML result processing", analysisId);
            return;
        }

        if (result.getClasses() == null || result.getClasses().isEmpty()) {
            log.warn("ML result for job #{} contains no class predictions", analysisId);
            return;
        }

        int savedBugs = 0;
        int savedSatd = 0;
        int savedDebtScores = 0;

        for (MLClassResultDTO classResult : result.getClasses()) {
            Long classId = classResult.getClassId();
            if (classId == null) {
                continue;
            }

            Class_Metrics classMetric = classMetricsRepository.findById(classId).orElse(null);
            if (classMetric == null) {
                log.warn("Class_Metrics #{} not found for job #{}", classId, analysisId);
                continue;
            }

            // 1. Save Bug Prediction
            MLBugPredictionDTO bugDto = classResult.getBugPrediction();
            double bugProb = 0.0;
            if (bugDto != null) {
                bugProb = Math.max(0.0, Math.min(1.0, bugDto.getProbabilityScore()));
                Bug_Prediction bugPrediction = bugPredictionRepository
                        .findByClassMetricsClassId(classId)
                        .orElseGet(Bug_Prediction::new);

                bugPrediction.setClassMetrics(classMetric);
                bugPrediction.setProbabilityScore(bugProb);
                bugPredictionRepository.save(bugPrediction);
                savedBugs++;
            }

            // 2. Save SATD Detections
            int classSatdCount = 0;
            if (classResult.getSatdDetections() != null) {
                for (MLSatdDetectionDTO satdDto : classResult.getSatdDetections()) {
                    Long commentId = satdDto.getCommentId();
                    if (commentId == null) {
                        continue;
                    }

                    Class_Comment comment = classCommentRepository.findById(commentId).orElse(null);
                    if (comment != null) {
                        SATD_Detection satdDetection = satdDetectionRepository
                                .findByClassCommentCommentId(commentId)
                                .orElseGet(SATD_Detection::new);

                        satdDetection.setClassComment(comment);
                        satdDetection.setCategory(satdDto.getCategory() != null ? satdDto.getCategory() : "WITHOUT_CLASSIFICATION");
                        double confidence = Math.max(0.0, Math.min(1.0, satdDto.getConfidenceScore()));
                        satdDetection.setConfidenceScore(confidence);

                        satdDetectionRepository.save(satdDetection);
                        savedSatd++;

                        if (satdDto.isDebt()) {
                            classSatdCount++;
                        }
                    }
                }
            }

            // 3. Compute and Save Multi-Dimensional Technical Debt Score
            com.debtlens.backend.engine.DebtAssessment assessment = debtScoreEngine.calculateDebtScore(
                    classMetric,
                    bugProb,
                    classResult.getSatdDetections()
            );

            Debt_Score debtScore = debtScoreRepository
                    .findByClassMetricsClassId(classId)
                    .orElseGet(Debt_Score::new);

            debtScore.setClassMetrics(classMetric);
            debtScore.setTechnicalDebtScore(assessment.getTechnicalDebtScore());
            debtScore.setHealthScore(assessment.getHealthScore());
            debtScore.setRiskLevel(assessment.getRiskLevel());
            debtScoreRepository.save(debtScore);
            savedDebtScores++;
        }

        // 4. Mark job as COMPLETED now that metrics + ML predictions + debt scores are persisted
        job.setStatus(AnalysisJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        analysisJobRepository.save(job);

        // 5. Update Status History
        Analysis_Status_History history = new Analysis_Status_History();
        history.setAnalysisJob(job);
        history.setStatus(AnalysisJobStatus.COMPLETED);
        history.setMessage("Analysis and ML pipeline completed: Persisted " + savedBugs + " bug predictions, "
                + savedSatd + " SATD classifications, and " + savedDebtScores + " debt scores.");
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);

        log.info("Finished processing ML results for job #{}: marked COMPLETED with {} bugs, {} SATD detections, {} debt scores",
                analysisId, savedBugs, savedSatd, savedDebtScores);
    }
}