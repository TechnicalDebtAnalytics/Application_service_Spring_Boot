package com.debtlens.backend.integration.rabbitmq;

public class AnalysisResultConsumer {
}

/*AnalysisResultConsumer.java

Consumes:

Analysis Worker
       ↓
Analysis Result Queue
       ↓
Application Backend

It receives:

Class metrics
Git metrics
Comments
Analysis status*/