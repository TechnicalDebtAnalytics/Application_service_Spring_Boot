package com.debtlens.backend.controller;

public class AnalysisController {
}
/*AnalysisController.java

This handles analysis-related REST APIs.

For example:

POST /repositories/{id}/analysis
GET  /analysis/{id}
GET  /analysis/{id}/status

When the user starts analysis:

React
 ↓
AnalysisController
 ↓
AnalysisService
 ↓
Create AnalysisJob
 ↓
AnalysisJobRepository
 ↓
AnalysisJobProducer
 ↓
RabbitMQ

The controller should not directly publish to RabbitMQ.*/