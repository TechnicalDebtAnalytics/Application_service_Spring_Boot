package com.debtlens.backend.integration.rabbitmq;

import com.debtlens.backend.config.RabbitMQConfig;
import com.debtlens.backend.dto.messaging.MLJobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MLJobProducer {

    private static final Logger log = LoggerFactory.getLogger(MLJobProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public MLJobProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishMLJob(MLJobMessage message) {
        log.info("Publishing ML prediction job #{} ({} classes) to queue '{}'",
                message.getJobId(),
                message.getClasses() != null ? message.getClasses().size() : 0,
                RabbitMQConfig.ML_JOB_CREATION_QUEUE
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.ML_JOB_CREATION_QUEUE, message);

        log.info("Successfully published ML job #{} to RabbitMQ", message.getJobId());
    }
}