package com.debtlens.backend.integration.rabbitmq;

import com.debtlens.backend.config.RabbitMQConfig;
import com.debtlens.backend.dto.messaging.AnalysisResultDTO;
import com.debtlens.backend.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisResultConsumer.class);

    private final AnalysisService analysisService;

    public AnalysisResultConsumer(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_RESULT_QUEUE)
    public void consumeAnalysisResult(AnalysisResultDTO result) {
        log.info("Received analysis result from queue '{}' for jobId: {}, status: {}",
                RabbitMQConfig.ANALYSIS_RESULT_QUEUE, result.getJobId(), result.getStatus());

        try {
            analysisService.processAnalysisResult(result);
        } catch (Exception e) {
            log.error("Failed to process analysis result for jobId {}: {}", result.getJobId(), e.getMessage(), e);
        }
    }
}