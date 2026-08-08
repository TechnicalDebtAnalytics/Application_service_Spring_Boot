package com.debtlens.backend.websocket;

public class AnalysisProgressPublisher {
}

/*This publishes analysis progress to your React frontend.

For example:

Analysis Worker
      ↓
RabbitMQ
      ↓
Application Backend
      ↓
AnalysisProgressPublisher
      ↓
WebSocket
      ↓
React

The UI could receive:

10% - Repository cloned
30% - Java parsing
50% - Metrics extracted
70% - ML processing
90% - Debt calculation
100% - Completed

This class should not perform the analysis itself.

It only communicates progress.*/