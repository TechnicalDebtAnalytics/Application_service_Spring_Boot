package com.debtlens.backend.integration.rabbitmq;

import com.debtlens.backend.config.RabbitMQConfig;
import com.debtlens.backend.dto.messaging.AnalysisJobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalysisJobProducer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public AnalysisJobProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAnalysisJob(AnalysisJobMessage jobMessage) {
        log.info("Publishing analysis job message to queue '{}': jobId={}, repoId={}",
                RabbitMQConfig.ANALYSIS_JOB_QUEUE, jobMessage.getJobId(), jobMessage.getRepositoryId());

        rabbitTemplate.convertAndSend(RabbitMQConfig.ANALYSIS_JOB_QUEUE, jobMessage);
    }
}