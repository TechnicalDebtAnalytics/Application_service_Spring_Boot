package com.debtlens.backend.integration.rabbitmq;

public class AnalysisJobProducer {
}

/*AnalysisJobProducer.java

Publishes:

Application Backend
       ↓
Analysis Job Queue
       ↓
Analysis Spring Boot Worker

It sends something like:

analysisJobId
repositoryId
repository URL
branch*/