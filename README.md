# Chatter - Real-Time Communication Platform

## Overview
A scalable, real-time messaging platform inspired by Discord, designed for instant direct messaging and robust channel discussions. Built with a focus on high availability and fault tolerance, this project leverages a microservices architecture to ensure seamless communication and efficient data management.

## Key Features & Architecture

* **Domain-Driven Design (DDD) & Clean Architecture:** The backend is logically separated into distinct microservices (`identity-access`, `profile`, `messaging`) within a multi-module workspace. This enforces strict architectural boundaries and centralizes configurations.
* **Real-Time Communication:** Instant messaging and live status updates are powered by **WebSockets**, ensuring low-latency interactions across the platform.
* **Optimized Chat History:** Chat logs are managed using **ScyllaDB** with a custom time-based bucketing strategy and active bucket tracking. Combined with **Snowflake IDs**, this ensures highly efficient query performance for real-time messaging streams.
* **Event-Driven & Eventual Consistency:** Reliable microservice communication is achieved by implementing the **Transactional Outbox pattern**. We utilize **Debezium CDC** (Change Data Capture) and **Apache Kafka** to guarantee message delivery without distributed locking.
* **Smart Media Management:** Implemented a two-phase, direct client-to-cloud media upload workflow. By utilizing presigned URLs and an explicit "commit" logic with **Cloudinary**, the system optimizes server bandwidth and actively prevents the accumulation of orphaned files.
* **Centralized Security:** Automated database credential rotation and sensitive configuration management are handled securely via **HashiCorp Vault**.
* **Advanced Authentication:** User access is secured with **JWT**, featuring robust refresh token rotation and versioning to prevent replay attacks and token theft.
* **Fault-Tolerant Infrastructure:** The system gracefully handles failures and traffic spikes using **Resilience4j** and **Bucket4j**, providing robust circuit breaking, rate limiting, and automatic retries.

## Technology Stack

### Backend & Frameworks
* **Java** / **Spring Boot** (Core Framework)
* **Spring Data JDBC / JPA** (Persistence)
* **Spring Cloud Eureka** (Service Discovery)
* **WebSockets** (Real-time communication)
* **Thymeleaf** (Template Engine)

### Databases & Messaging
* **PostgreSQL** (Relational Data / Core Domain)
* **ScyllaDB** (High-throughput Message Storage)
* **Redis** (Caching & Session Management)
* **Apache Kafka** (Message Broker)
* **Debezium** (Change Data Capture)
* **Flyway** (Database Migrations)

### Infrastructure, Security & Observability
* **Docker** (Containerization)
* **HashiCorp Vault** (Secrets Management)
* **Cloudinary** (Media Storage Provider)
* **Resilience4j & Bucket4j** (Reliability & Rate Limiting)
* **Log4j** (Comprehensive Application Logging with automated daily rotation)
* **Swagger/OpenAPI** (API Documentation)

## Getting Started


### Prerequisites
* Java 17+
* Docker & Docker Compose
* Cloudinary Account
* Maven/Gradle

### Docker and setup
* Navigate to each folder within `/infrastructure` and `/services` (excluding `services/service-registry`) and check their respective `README.md` files.
* Execute the commands listed in the **Run with Docker** section.
* Please make sure to follow any additional setup instructions or guides provided in those README files.