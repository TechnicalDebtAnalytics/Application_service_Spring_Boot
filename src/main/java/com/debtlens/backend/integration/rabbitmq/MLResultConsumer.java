package com.debtlens.backend.integration.rabbitmq;

public class MLResultConsumer {
}

/*MLResultConsumer.java

Consumes:

FastAPI
       ↓
ML Result Queue
       ↓
Application Backend

It receives:

SATD predictions
Bug predictions
Probabilities*/