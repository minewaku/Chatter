package com.minewaku.chatter.adapter.messaging.publisher;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.config.properties.DebeziumProperties;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DebeziumOutboxEventListener implements SmartLifecycle {

    private final OutboxEventPublisher publisher;
    private final DebeziumProperties debeziumProperties;

    // Quản lý thread và engine
    private ExecutorService executor;
    private DebeziumEngine<ChangeEvent<String, String>> engine;
    
    // Cờ trạng thái
    private volatile boolean running = false;

    public DebeziumOutboxEventListener(
            OutboxEventPublisher publisher,
            DebeziumProperties debeziumProperties) {

        this.publisher = publisher;
        this.debeziumProperties = debeziumProperties;
    }

    @Override
    public void start() {
        if (this.running) {
            return;
        }

        this.engine = DebeziumEngine.create(Json.class)
                .using(debeziumProperties.getProps())
                .notifying(record -> {
                    try {
                        publisher.publish(record.value());
                    } catch (Exception e) {
                        log.error("Error processing Debezium event: ", e);
                    }
                })
                .using((success, message, error) -> {
                    if (error != null) {
                        log.error("Debezium Engine Error: " + message);
                    }
                })
                .build();

        this.executor = Executors.newSingleThreadExecutor();
        this.executor.submit(this.engine);
        
        this.running = true;
        log.info("Debezium Engine Started Successfully via SmartLifecycle");
    }

    @Override
    public void stop() {
        if (!this.running) {
            return;
        }

        System.out.println("Stopping Debezium Engine...");
        try {
            if (this.engine != null) {
                this.engine.close();
            }
            
            // Shutdown thread pool
            if (this.executor != null) {
                this.executor.shutdown();
                // Chờ một chút cho thread dừng hẳn
                if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        
        this.running = false;
        System.out.println("Debezium Engine Stopped.");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}