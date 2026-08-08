package com.debtlens.backend.config;

public class RabbitMQConfig {
}
//This is where you configure RabbitMQ infrastructure.
//
//You have four queues:
//
//analysis.job.queue
//analysis.result.queue
//ml.job.queue
//ml.result.queue
//
//You configure:
//
//Connection
//        Exchanges
//Queues
//Routing keys
//Bindings
//Message converters
//Listener configuration if needed