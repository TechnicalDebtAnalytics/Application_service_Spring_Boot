package com.debtlens.backend.controller;

public class ReportController {
}
/*. ReportController.java

Responsible for retrieving analysis reports.

For example:

GET /repositories/{id}/report
GET /repositories/{id}/debt-score
GET /repositories/{id}/recommendations
GET /reports/{id}

Flow:

React
 ↓
ReportController
 ↓
ReportService
 ↓
ReportRepository
 ↓
PostgreSQL*/