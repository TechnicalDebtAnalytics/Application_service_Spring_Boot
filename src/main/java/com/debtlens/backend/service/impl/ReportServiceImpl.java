package com.debtlens.backend.service.impl;

public class ReportServiceImpl {
}
/*eportServiceImpl.java

This is particularly important because your final debt calculation happens here or in a dedicated domain/business component.

Your flow:

Metrics
+
ML Predictions
+
SATD results
        ↓
Debt Score Calculation
        ↓
Class-level Debt Score
        ↓
Recommendation Generation
        ↓
Technical Debt Report

The Application Backend owns this business logic.*/