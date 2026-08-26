package com.debtlens.backend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ANALYSIS_JOB_QUEUE = "analysis_job_creation.queue";
    public static final String ANALYSIS_RESULT_QUEUE = "analysis_job_results.queue";
    public static final String ML_JOB_CREATION_QUEUE = "ML_job_cretion.queue";
    public static final String ML_JOB_RESULTS_QUEUE = "ML_job_results.queue";

    @Bean
    public Queue analysisJobCreationQueue() {
        return new Queue(ANALYSIS_JOB_QUEUE, true);
    }

    @Bean
    public Queue analysisJobResultsQueue() {
        return new Queue(ANALYSIS_RESULT_QUEUE, true);
    }

    @Bean
    public Queue mlJobCreationQueue() {
        return new Queue(ML_JOB_CREATION_QUEUE, true);
    }

    @Bean
    public Queue mlJobResultsQueue() {
        return new Queue(ML_JOB_RESULTS_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}