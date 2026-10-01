package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.SystemHealthItemDTO;
import com.debtlens.backend.dto.response.SystemHealthResponseDTO;
import com.debtlens.backend.service.SystemHealthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {
    private static final Logger log = LoggerFactory.getLogger(SystemHealthServiceImpl.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String analysisHealthUrl;
    private final String mlHealthUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public SystemHealthServiceImpl(DataSource dataSource, ConnectionFactory rabbitConnectionFactory,
                                   @Value("${services.analysis.health-url:http://localhost:8082/actuator/health}") String analysisHealthUrl,
                                   @Value("${services.ml.health-url:http://localhost:8000/health}") String mlHealthUrl) {
        this.dataSource = dataSource;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.analysisHealthUrl = analysisHealthUrl;
        this.mlHealthUrl = mlHealthUrl;
    }

    @Override
    public SystemHealthResponseDTO getSystemHealth() {
        LocalDateTime checkedAt = LocalDateTime.now();
        SystemHealthItemDTO application = new SystemHealthItemDTO("Application Service", "backend",
                "Spring Boot API", "UP", "REST API is responding", 0, checkedAt);
        CompletableFuture<SystemHealthItemDTO> database = CompletableFuture.supplyAsync(() -> databaseHealth(checkedAt));
        CompletableFuture<SystemHealthItemDTO> rabbit = CompletableFuture.supplyAsync(() -> rabbitHealth(checkedAt));
        CompletableFuture<SystemHealthItemDTO> analysis = CompletableFuture.supplyAsync(() -> remoteHealth(
                "Repository Analysis Service", "analysis_service", "Static analysis worker", analysisHealthUrl, checkedAt));
        CompletableFuture<SystemHealthItemDTO> ml = CompletableFuture.supplyAsync(() -> remoteHealth(
                "Machine Learning Service", "ml_service", "SATD and defect prediction", mlHealthUrl, checkedAt));

        List<SystemHealthItemDTO> services = List.of(application, analysis.join(), ml.join(), rabbit.join(), database.join());
        boolean allUp = services.stream().allMatch(item -> "UP".equals(item.status()));
        boolean anyUp = services.stream().anyMatch(item -> "UP".equals(item.status()));
        return new SystemHealthResponseDTO(allUp ? "UP" : anyUp ? "DEGRADED" : "DOWN", checkedAt, services);
    }

    private SystemHealthItemDTO databaseHealth(LocalDateTime checkedAt) {
        long start = System.nanoTime();
        try (java.sql.Connection connection = dataSource.getConnection()) {
            boolean healthy = connection.isValid(2);
            return item("PostgreSQL Database", "database", "Primary data store", healthy ? "UP" : "DOWN",
                    healthy ? "Database connection validated" : "Database validation failed", start, checkedAt);
        } catch (Exception exception) {
            log.warn("Database health check failed: {}", exception.getMessage());
            return item("PostgreSQL Database", "database", "Primary data store", "DOWN",
                    "Database connection unavailable", start, checkedAt);
        }
    }

    private SystemHealthItemDTO rabbitHealth(LocalDateTime checkedAt) {
        long start = System.nanoTime();
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            boolean healthy = connection.isOpen();
            return item("RabbitMQ", "rabbitmq", "Analysis message broker", healthy ? "UP" : "DOWN",
                    healthy ? "Broker connection validated" : "Broker connection unavailable", start, checkedAt);
        } catch (Exception exception) {
            log.warn("RabbitMQ health check failed: {}", exception.getMessage());
            return item("RabbitMQ", "rabbitmq", "Analysis message broker", "DOWN",
                    "Broker connection unavailable", start, checkedAt);
        }
    }

    private SystemHealthItemDTO remoteHealth(String name, String key, String description, String url,
                                              LocalDateTime checkedAt) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return item(name, key, description, "DOWN", "Health endpoint returned HTTP " + response.statusCode(),
                        start, checkedAt);
            }
            JsonNode root = objectMapper.readTree(response.body());
            String remoteStatus = root.path("status").asText("").toUpperCase();
            String status = switch (remoteStatus) {
                case "UP" -> "UP";
                case "DEGRADED" -> "DEGRADED";
                default -> "DOWN";
            };
            String details = switch (status) {
                case "UP" -> "Service health endpoint is ready";
                case "DEGRADED" -> "Service is responding with reduced readiness";
                default -> "Service returned an invalid or unavailable status";
            };
            return item(name, key, description, status, details, start, checkedAt);
        } catch (Exception exception) {
            log.warn("{} health check failed: {}", name, exception.getMessage());
            return item(name, key, description, "DOWN", "Health endpoint unavailable", start, checkedAt);
        }
    }

    private SystemHealthItemDTO item(String name, String key, String description, String status, String details,
                                     long startedAtNanos, LocalDateTime checkedAt) {
        long elapsed = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
        return new SystemHealthItemDTO(name, key, description, status, details, elapsed, checkedAt);
    }
}
