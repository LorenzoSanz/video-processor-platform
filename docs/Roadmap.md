# Video Processing Platform

# Roadmap

> Living document.
> Every phase represents a real evolution of the system. New technologies are only introduced when they solve an existing limitation.

---

# Progress

| Phase                               | Status         |
| ----------------------------------- | -------------- |
| Phase 1 - Upload Service            | 🚧 In Progress |
| Phase 2 - Persistence               | ⏳ Planned      |
| Phase 3 - Object Storage            | ⏳ Planned      |
| Phase 4 - Job Queue                 | ⏳ Planned      |
| Phase 5 - Workers                   | ⏳ Planned      |
| Phase 6 - Video Processing Pipeline | ⏳ Planned      |
| Phase 7 - Cache                     | ⏳ Planned      |
| Phase 8 - Containerization          | ⏳ Planned      |
| Phase 9 - Observability             | ⏳ Planned      |
| Phase 10 - Load Testing             | ⏳ Planned      |
| Phase 11 - Kubernetes               | ⏳ Planned      |
| Phase 12 - CI/CD                    | ⏳ Planned      |
| Phase 13 - Linux & Systems          | ⏳ Planned      |

---

# Phase 1 — Upload Service

Goal:

Build a robust monolithic upload service with clean architecture.

## Features

* [x] Layered architecture
* [x] DTOs
* [x] Upload entity
* [x] Repository layer
* [x] Upload endpoint
* [x] Local storage
* [x] File validation
* [x] Magic Number validation
* [x] Storage abstraction
* [x] Custom exceptions
* [x] Compensation pattern
* [x] Transaction management

## Remaining

* [ ] Global Exception Handler
* [ ] Structured logging
* [ ] Unit tests
* [ ] Integration tests
* [ ] Swagger / OpenAPI
* [ ] Validation documentation

### Concepts learned

* Dependency Injection
* Interfaces
* SOLID
* Transactions
* Exception handling
* Compensation pattern

---

# Phase 2 — Persistence

Goal:

Turn the application into a production-ready persistence layer.

## Features

* [ ] PostgreSQL
* [ ] Flyway migrations
* [ ] Database indexes
* [ ] Optimistic locking
* [ ] Auditing
* [ ] Pagination
* [ ] Sorting

### Concepts learned

* Database migrations
* Concurrency
* Indexing
* Production persistence

---

# Phase 3 — Object Storage

Current limitation

Files are stored locally.

Goal

Move storage to object storage.

## Features

* [ ] MinIO
* [ ] Buckets
* [ ] Presigned URLs
* [ ] Streaming uploads
* [ ] Streaming downloads

### Concepts learned

* Object Storage
* Cloud storage
* Streaming

---

# Phase 4 — Job Queue

Current limitation

Everything is synchronous.

Goal

Introduce asynchronous processing.

## Features

* [ ] RabbitMQ
* [ ] Producer
* [ ] Consumer
* [ ] ACK
* [ ] Retry
* [ ] Dead Letter Queue
* [ ] Exponential Backoff

### Concepts learned

* Message queues
* Event-driven thinking
* Reliability
* Resilience

---

# Phase 5 — Workers

Current limitation

One service performs all tasks.

Goal

Split processing into independent workers.

## Workers

* [ ] Thumbnail Worker
* [ ] Metadata Worker
* [ ] Virus Scan Worker
* [ ] Notification Worker

### Concepts learned

* Distributed systems
* Worker architecture
* Scalability

---

# Phase 6 — Video Processing Pipeline

Current limitation

Only file uploads exist.

Goal

Build a complete processing pipeline.

## Features

* [ ] Chunk upload
* [ ] Chunk assembler
* [ ] Metadata extraction
* [ ] Thumbnail generation
* [ ] FFmpeg integration
* [ ] Video transcoding
* [ ] Preview generation

### Concepts learned

* Pipelines
* Parallel processing
* Large file handling

---

# Phase 7 — Cache

Current limitation

Everything hits the database.

Goal

Introduce Redis.

## Features

* [ ] Redis
* [ ] Response cache
* [ ] Metadata cache
* [ ] Rate limiting
* [ ] Distributed locks

### Concepts learned

* Caching
* Performance
* Distributed coordination

---

# Phase 8 — Containerization

Goal

Package the platform.

## Features

* [ ] Docker
* [ ] Docker Compose
* [ ] Multi-container environment

### Services

* Upload API
* PostgreSQL
* RabbitMQ
* Redis
* MinIO

### Concepts learned

* Containers
* Networking
* Multi-service development

---

# Phase 9 — Observability

Goal

Understand the system.

## Features

* [ ] Prometheus
* [ ] Grafana
* [ ] Loki
* [ ] OpenTelemetry

### Metrics

* Upload duration
* Worker duration
* Queue size
* Failed jobs
* Retry count
* Active workers

### Concepts learned

* Monitoring
* Logging
* Tracing

---

# Phase 10 — Load Testing

Goal

Measure scalability.

## Features

* [ ] k6 load testing
* [ ] Concurrent uploads
* [ ] Queue saturation
* [ ] Worker throughput
* [ ] Bottleneck analysis

### Concepts learned

* Performance engineering
* Capacity planning

---

# Phase 11 — Kubernetes

Goal

Run the platform in a cluster.

## Features

* [ ] Deployments
* [ ] Services
* [ ] ConfigMaps
* [ ] Secrets
* [ ] Horizontal Pod Autoscaler
* [ ] Rolling Updates

### Concepts learned

* Orchestration
* Scaling
* Production deployments

---

# Phase 12 — CI/CD

Goal

Automate delivery.

## Features

* [ ] GitHub Actions
* [ ] Build
* [ ] Tests
* [ ] Docker image
* [ ] Automatic deployment

### Concepts learned

* Continuous Integration
* Continuous Delivery

---

# Phase 13 — Linux & Systems

This phase is complementary to the project.

The goal is to understand what happens underneath the technologies already implemented.

## Linux

* [ ] Processes
* [ ] Threads
* [ ] Scheduler
* [ ] Memory
* [ ] File System
* [ ] Networking
* [ ] epoll

## C

* [ ] upload-cli
* [ ] File hashing
* [ ] Binary parsing
* [ ] Socket programming

## Kernel

* [ ] read()
* [ ] write()
* [ ] mmap()
* [ ] sendfile()
* [ ] System Calls
* [ ] VFS

---

# Final Platform

```text
                Client
                   │
                   ▼
             Upload API
                   │
          ┌────────┴────────┐
          ▼                 ▼
      PostgreSQL         MinIO
          │
          ▼
       RabbitMQ
          │
 ┌────────┼────────┬────────┬────────┐
 ▼        ▼        ▼        ▼
Thumbnail Metadata Virus Notification
 Worker    Worker  Worker     Worker
          │
          ▼
     Prometheus
          │
          ▼
      Grafana/Loki
```

---

# Final Goal

Build a production-inspired distributed backend platform that demonstrates practical experience in:

* Backend Engineering
* Distributed Systems
* Cloud Architecture
* Object Storage
* Message Queues
* Worker Processing
* Resilience
* Observability
* Scalability
* Containerization
* Kubernetes
* Performance Engineering
