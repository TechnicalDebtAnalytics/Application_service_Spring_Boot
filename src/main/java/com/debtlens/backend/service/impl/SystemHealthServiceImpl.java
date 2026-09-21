package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.SystemHealthItemDTO;
import com.debtlens.backend.dto.response.SystemHealthResponseDTO;
import com.debtlens.backend.service.SystemHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthServiceImpl.class);

    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;

    public SystemHealthServiceImpl(DataSource dataSource, ConnectionFactory rabbitConnectionFactory) {
        this.dataSource = dataSource;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    @Override
    public SystemHealthResponseDTO getSystemHealth() {
        List<SystemHealthItemDTO> services = new ArrayList<>();

        // 1. Application Service (Self)
        services.add(new SystemHealthItemDTO(
                "Application Service",
                "backend",
                "Spring Boot backend",
                "UP",
                "REST API service operational"
        ));

        // 2. Machine Learning Service
        boolean rabbitHealthy = checkRabbitMQHealth();

        // 2. Machine Learning Service
        services.add(new SystemHealthItemDTO(
                "Machine Learning Service",
                "ml_service",
                "SATD and defect prediction",
                rabbitHealthy ? "UP" : "DOWN",
                rabbitHealthy ? "Worker engine operational" : "Message broker unreachable"
        ));

        // 3. Repository Analysis Service
        services.add(new SystemHealthItemDTO(
                "Repository Analysis Service",
                "analysis_service",
                "Static analysis worker",
                rabbitHealthy ? "UP" : "DOWN",
                rabbitHealthy ? "Analysis worker queue active" : "Message broker unreachable"
        ));

        // 4. RabbitMQ
        services.add(new SystemHealthItemDTO(
                "RabbitMQ",
                "rabbitmq",
                "Analysis message queue",
                rabbitHealthy ? "UP" : "DOWN",
                rabbitHealthy ? "AMQP broker connected" : "Connection failed"
        ));

        // 5. Neon Database
        boolean dbHealthy = checkDatabaseHealth();
        services.add(new SystemHealthItemDTO(
                "Neon Database",
                "database",
                "PostgreSQL database",
                dbHealthy ? "UP" : "DOWN",
                dbHealthy ? "PostgreSQL database connected" : "JDBC connection failed"
        ));

        // Determine overall status
        boolean allUp = services.stream().allMatch(s -> "UP".equalsIgnoreCase(s.status()));
        boolean anyUp = services.stream().anyMatch(s -> "UP".equalsIgnoreCase(s.status()));

        String overallStatus = allUp ? "UP" : (anyUp ? "DEGRADED" : "DOWN");

        return new SystemHealthResponseDTO(
                overallStatus,
                LocalDateTime.now(),
                services
        );
    }

    private boolean checkDatabaseHealth() {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRabbitMQHealth() {
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            return connection.isOpen();
        } catch (Exception e) {
            log.error("RabbitMQ health check failed: {}", e.getMessage());
            return false;
        }
    }
}
