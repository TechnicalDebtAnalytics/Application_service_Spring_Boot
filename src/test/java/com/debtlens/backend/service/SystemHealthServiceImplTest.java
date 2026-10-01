package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.SystemHealthResponseDTO;
import com.debtlens.backend.service.impl.SystemHealthServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthServiceImplTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void reportsWorkersIndependentlyAndMarksPartialReadinessDegraded() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/analysis", exchange -> respond(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/ml", exchange -> respond(exchange, 200, "{\"status\":\"DEGRADED\"}"));
        server.start();

        DataSource dataSource = mock(DataSource.class);
        java.sql.Connection db = mock(java.sql.Connection.class);
        when(dataSource.getConnection()).thenReturn(db); when(db.isValid(2)).thenReturn(true);
        ConnectionFactory rabbitFactory = mock(ConnectionFactory.class);
        org.springframework.amqp.rabbit.connection.Connection rabbit = mock(org.springframework.amqp.rabbit.connection.Connection.class);
        when(rabbitFactory.createConnection()).thenReturn(rabbit); when(rabbit.isOpen()).thenReturn(true);
        String base = "http://localhost:" + server.getAddress().getPort();

        SystemHealthResponseDTO result = new SystemHealthServiceImpl(dataSource, rabbitFactory,
                base + "/analysis", base + "/ml").getSystemHealth();

        assertEquals("DEGRADED", result.overallStatus());
        assertEquals("UP", result.services().stream().filter(item -> item.key().equals("analysis_service")).findFirst().orElseThrow().status());
        assertEquals("DEGRADED", result.services().stream().filter(item -> item.key().equals("ml_service")).findFirst().orElseThrow().status());
    }

    @Test
    void malformedAndUnavailableWorkerResponsesAreDown() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/analysis", exchange -> respond(exchange, 200, "not-json"));
        server.createContext("/ml", exchange -> respond(exchange, 503, "{}"));
        server.start();
        DataSource dataSource = mock(DataSource.class); when(dataSource.getConnection()).thenThrow(new RuntimeException("offline"));
        ConnectionFactory rabbitFactory = mock(ConnectionFactory.class); when(rabbitFactory.createConnection()).thenThrow(new RuntimeException("offline"));
        String base = "http://localhost:" + server.getAddress().getPort();

        SystemHealthResponseDTO result = new SystemHealthServiceImpl(dataSource, rabbitFactory,
                base + "/analysis", base + "/ml").getSystemHealth();

        assertEquals("DEGRADED", result.overallStatus());
        assertEquals(4, result.services().stream().filter(item -> item.status().equals("DOWN")).count());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
