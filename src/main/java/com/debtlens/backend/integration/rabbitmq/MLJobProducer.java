package com.debtlens.backend.integration.rabbitmq;

public class MLJobProducer {
}

/*MLJobProducer.java

Publishes:

Application Backend
       ↓
ML Job Queue
       ↓
FastAPI

It sends the required data/features for ML processing.*/