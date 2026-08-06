# Video Processing Platform

# Architecture

> **Version:** 1.0
> **Status:** In Development

---

# Table of Contents

1. Introduction
2. Platform Overview
3. Architecture Goals
4. Design Principles
5. Current Architecture
6. Components
7. Current Upload Flow
8. Architecture Evolution
9. Target Architecture
10. Persistence
11. Object Storage
12. Messaging
13. Workers
14. Cache
15. Observability
16. Resilience
17. Scalability
18. Conventions
19. Architecture Decision Records (ADR)

---

# 1. Introduction

## Purpose of this document

This document describes the architecture of the **Video Processing Platform**.

It explains not only **how** the system is implemented, but more importantly **why** every architectural decision has been made.

Unlike API documentation or implementation details, this document focuses on the overall design of the platform and the evolution of the system throughout its development.

It should serve as the main technical reference for understanding the project.

---

## Project Overview

The Video Processing Platform is a backend system designed to receive, store and process large media files using modern backend engineering practices.

Although the project starts as a relatively small upload service, it is intentionally designed to evolve into a distributed platform capable of processing video asynchronously through multiple independent workers.

The platform will gradually incorporate technologies commonly used in production environments, such as object storage, message queues, distributed workers, containerization, orchestration, monitoring and cloud-native infrastructure.

---

## Philosophy

The primary objective of this project is **learning through solving real engineering problems**.

New technologies are never introduced simply because they are popular.

Instead, every new tool must solve an actual limitation that appears during the evolution of the system.

This means that the architecture grows naturally:

* A new component is introduced only when the current design becomes insufficient.
* Every architectural decision should have a clear justification.
* Simplicity is preferred over unnecessary complexity.
* Code should remain understandable as the platform grows.

---

## Educational Objectives

Beyond implementing features, this project aims to develop a deep understanding of modern backend engineering.

The platform is intended to provide practical experience with topics such as:

* Clean software architecture.
* Object-oriented design.
* Distributed systems.
* Cloud-native applications.
* Scalability.
* Resilience.
* Performance.
* Observability.
* Containerized deployments.
* Production-ready backend development.

Rather than memorizing technologies, the goal is to understand the engineering principles that make those technologies necessary.

---

## Scope

At the time of writing, the project is still in its early stages.

The current implementation focuses on a robust upload service with proper validation, persistence and storage abstraction.

Future iterations will progressively introduce asynchronous processing, object storage, distributed workers, monitoring, orchestration and production infrastructure without requiring major architectural rewrites.

This incremental approach allows each new feature to build naturally upon the existing architecture while preserving maintainability and code quality.

# 2. Platform Overview

## High-Level Vision

The Video Processing Platform is designed as a distributed backend capable of receiving, storing and processing media files asynchronously.

Although the first version consists of a single upload service, the final architecture will be composed of multiple specialized components working together.

Each component has a single responsibility and communicates with the others through well-defined interfaces or asynchronous messages.

The objective is to build a platform that resembles the architecture used in modern cloud applications while keeping each evolution incremental and easy to understand.

---

## High-Level Architecture

```text
                           Client
                              │
                              ▼
                     Upload REST API
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
              PostgreSQL            MinIO
                    │
                    ▼
                RabbitMQ
                    │
      ┌─────────────┼─────────────┬─────────────┐
      ▼             ▼             ▼             ▼
 Metadata      Thumbnail      Virus Scan   Notification
  Worker          Worker         Worker        Worker

                    │
                    ▼
              Prometheus
                    │
                    ▼
           Grafana / Loki
```

---

## Main Responsibilities

The platform is divided into independent responsibilities.

### Client

The client communicates with the platform through a REST API.

Its only responsibility is sending requests and receiving responses.

It has no knowledge of storage, workers or databases.

---

### Upload API

The Upload API acts as the platform's entry point.

Responsibilities:

* Receive upload requests.
* Validate incoming files.
* Store uploaded files.
* Persist upload metadata.
* Publish processing jobs.

The API never performs expensive background work itself.

---

### PostgreSQL

PostgreSQL stores the application's persistent data.

Examples:

* Upload information.
* Processing status.
* Metadata.
* Relationships between entities.

It is considered the source of truth for business information.

---

### MinIO

MinIO acts as the platform's object storage.

Instead of storing files inside the database, large binary objects are stored separately.

Advantages include:

* Better scalability.
* Lower database size.
* Faster access to large files.
* Compatibility with Amazon S3 APIs.

---

### RabbitMQ

RabbitMQ is responsible for asynchronous communication.

Instead of processing videos immediately after upload, the Upload API publishes jobs to RabbitMQ.

Workers consume those jobs independently.

This allows the platform to process many uploads simultaneously without blocking client requests.

---

### Workers

Workers execute background tasks.

Each worker is responsible for a single operation.

Examples include:

* Thumbnail generation.
* Metadata extraction.
* Virus scanning.
* Notifications.

Workers can be replicated horizontally without modifying the rest of the platform.

---

### Redis

Redis will be introduced later to improve performance.

Potential responsibilities include:

* Frequently accessed data cache.
* Distributed locking.
* Rate limiting.
* Temporary processing state.

Redis is not the primary database.

---

### Observability Stack

Production systems must be observable.

The platform will include:

* **Prometheus** for metrics collection.
* **Grafana** for visualization.
* **Loki** for centralized logging.
* **OpenTelemetry** for distributed tracing.

Together, these tools make it possible to understand the health and performance of the platform.

---

## Design Philosophy

The platform follows a simple rule:

> **Each component should have one clear responsibility.**

This separation reduces coupling, improves maintainability and allows each part of the system to evolve independently.

Examples:

* Storage does not know anything about HTTP.
* Controllers do not know anything about persistence.
* Workers do not communicate directly with clients.
* RabbitMQ only transports messages.
* PostgreSQL only stores business data.

Because every component has a well-defined purpose, replacing or extending one part of the platform has minimal impact on the others.

---

## Evolution Strategy

The platform is intentionally built in small, incremental steps.

The first versions prioritize correctness and simplicity.

As new limitations appear, additional technologies are introduced to solve those specific problems.

For example:

* Local storage will eventually become object storage.
* Synchronous processing will evolve into asynchronous workers.
* A single service will become a distributed architecture.
* Local execution will evolve into containerized deployments.
* Manual execution will evolve into automated CI/CD pipelines.

This evolutionary approach keeps the architecture understandable while progressively introducing real-world engineering concepts.

# 3. Architecture Goals

Architecture is not about using as many technologies as possible.

The objective of this project is to build a system that remains understandable, maintainable and scalable as new features are added.

Every architectural decision should solve a real problem.

---

# 3.1 Simplicity First

The simplest solution that correctly solves the current problem is always preferred.

Features are not implemented in anticipation of hypothetical future requirements.

Instead, the architecture evolves only when the existing design becomes insufficient.

This avoids unnecessary complexity while allowing the platform to grow naturally.

---

# 3.2 Separation of Responsibilities

Every component should have a single, well-defined responsibility.

Examples:

* Controllers handle HTTP.
* Services coordinate business logic.
* Repositories access the database.
* Storage implementations store files.
* Workers execute background jobs.

This separation makes the code easier to understand, test and modify.

Whenever possible, a class should have only one reason to change.

---

# 3.3 Loose Coupling

Components should know as little as possible about one another.

Instead of depending on concrete implementations, components communicate through abstractions.

For example:

```text id="6g2npr"
UploadService

        │

        ▼

StorageService (interface)

        │

        ▼

LocalStorageService
```

Because `UploadService` depends on the `StorageService` interface, replacing local storage with MinIO or Amazon S3 does not require modifications to the service layer.

Loose coupling allows the system to evolve without introducing unnecessary changes across unrelated components.

---

# 3.4 High Cohesion

While components should be loosely coupled, they should also be highly cohesive.

A class should contain functionality that naturally belongs together.

For example:

`LocalStorageService` is responsible for:

* Building storage paths.
* Creating directories.
* Writing files.
* Deleting files.

It is **not** responsible for:

* HTTP requests.
* Database persistence.
* Validation.
* Queue publishing.

Keeping related behaviour together improves readability and reduces duplication.

---

# 3.5 Replaceability

Every implementation should be replaceable whenever possible.

The platform is intentionally designed so that infrastructure components can change without affecting business logic.

Examples:

```text id="ehdtjl"
LocalStorageService

↓

MinIOStorageService

↓

AmazonS3StorageService
```

All of them implement the same contract.

The rest of the application remains unchanged.

This makes experimentation and future migrations significantly easier.

---

# 3.6 Scalability

The platform should be capable of growing without requiring fundamental architectural changes.

Initially, a single application is sufficient.

As demand increases, new components can be introduced:

* Message queues.
* Workers.
* Object storage.
* Containers.
* Kubernetes.

Because responsibilities are already separated, scaling individual parts of the platform becomes possible without redesigning the entire system.

---

# 3.7 Resilience

Failures are inevitable.

The architecture should minimise the impact of those failures.

Examples include:

* Rolling back database transactions.
* Cleaning orphaned files through compensation.
* Retrying failed background jobs.
* Dead Letter Queues for unrecoverable messages.

The goal is not to eliminate failures, but to ensure the system remains consistent when they occur.

---

# 3.8 Observability

A production system should explain what it is doing.

The platform will progressively incorporate:

* Metrics.
* Logs.
* Distributed tracing.
* Health checks.

Observability makes debugging and performance analysis possible without modifying application code.

---

# 3.9 Incremental Evolution

One of the most important goals of this project is demonstrating that complex systems do not need to be built all at once.

The platform evolves through a sequence of justified improvements.

For example:

```text id="z4g7yw"
Local Storage

↓

Object Storage

↓

Message Queue

↓

Workers

↓

Distributed Processing

↓

Containerization

↓

Kubernetes
```

Every new phase solves a limitation introduced by the previous one.

This mirrors how real software systems evolve over time.

---

# 3.10 Learning Through Engineering

This project is designed as a learning platform rather than a technology showcase.

Every concept is introduced only after the need for it has appeared naturally.

For example:

* Interfaces are introduced to decouple implementations.
* Transactions appear when persistence must remain consistent.
* RabbitMQ is introduced when synchronous processing becomes a bottleneck.
* Redis appears only when database access becomes inefficient.
* Kubernetes is introduced only after multiple independent services exist.

This approach ensures that every technology is understood within the context of a real engineering problem rather than as an isolated tool.

---

# Summary

The architecture of this platform is guided by a small set of principles:

* Keep the design simple.
* Separate responsibilities.
* Reduce coupling.
* Maximise cohesion.
* Depend on abstractions.
* Design for evolution.
* Build for scalability.
* Recover gracefully from failures.
* Introduce complexity only when it solves a real problem.

These principles influence every architectural decision made throughout the project.

# 4. Design Principles

The architecture of the platform is driven by a small number of engineering principles rather than by specific technologies.

These principles remain constant throughout the evolution of the project and influence every implementation decision.

---

# 4.1 Layered Architecture

The application is divided into layers, where each layer has a single responsibility.

```text
Client
   │
   ▼
Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
Database
```

Each layer communicates only with the layer immediately below it.

For example:

* A controller never accesses the database directly.
* A repository never returns HTTP responses.
* A storage implementation never performs business logic.

This separation keeps responsibilities clear and reduces coupling between unrelated parts of the application.

---

# 4.2 Single Responsibility Principle (SRP)

Every class should have one reason to change.

Examples from the current project:

| Class                    | Responsibility          |
| ------------------------ | ----------------------- |
| `UploadController`       | HTTP communication      |
| `UploadService`          | Upload workflow         |
| `UploadRepository`       | Database access         |
| `LocalStorageService`    | Physical file storage   |
| `FileValidator`          | General file validation |
| `FileSignatureValidator` | Magic Number validation |

Whenever a class starts accumulating unrelated responsibilities, it should be divided into smaller components.

---

# 4.3 Dependency Inversion

High-level business logic should not depend on infrastructure.

Instead of writing:

```java
UploadService
        ↓
LocalStorageService
```

the project uses:

```text
UploadService
        │
        ▼
StorageService
        │
        ▼
LocalStorageService
```

The service depends on an abstraction rather than on a concrete implementation.

This allows infrastructure to change without affecting business logic.

---

# 4.4 Programming to Interfaces

Interfaces define **what** a component can do.

Implementations decide **how** it does it.

For example:

```text
StorageService

storeFile()

deleteFile()
```

The Upload Service only knows that files can be stored.

It does not know:

* where,
* how,
* or by which technology.

This makes the platform extensible without modifying existing code.

---

# 4.5 Composition over Inheritance

Whenever possible, the project prefers composition instead of inheritance.

Example:

```text
UploadService

contains

StorageService
```

instead of

```text
UploadService

extends

StorageService
```

Composition produces more flexible systems and avoids tight coupling between classes.

Inheritance is reserved for situations where a genuine "is-a" relationship exists.

---

# 4.6 Fail Fast

Invalid states should be detected as early as possible.

Example:

```text
Receive file

↓

Validate request

↓

Validate Magic Numbers

↓

Store file

↓

Save metadata
```

The application validates the request before performing expensive operations.

This avoids unnecessary work and prevents invalid data from propagating through the system.

---

# 4.7 Defensive Programming

The platform never assumes external input is correct.

Incoming files are treated as untrusted.

Examples include:

* File size validation.
* MIME type validation.
* Magic Number validation.
* Exception handling.
* Safe file deletion.

The system should always behave predictably, even when receiving invalid input.

---

# 4.8 Explicit Error Handling

Errors should never disappear silently.

Unexpected situations must either:

* be handled,
* or be propagated.

Current examples:

* Validation exceptions.
* Storage exceptions.
* Database exceptions.

Each exception represents a specific business or infrastructure problem.

The responsibility for converting exceptions into HTTP responses belongs exclusively to the web layer.

---

# 4.9 Incremental Complexity

Complexity should appear only when necessary.

The current architecture intentionally starts small.

Future technologies are introduced only after they solve an existing limitation.

Example:

```text
Local Storage

↓

Object Storage

↓

Message Queue

↓

Workers

↓

Distributed Platform
```

Each evolution has a clear engineering motivation.

---

# 4.10 Technology Independence

Business logic should remain independent of infrastructure.

For example:

The Upload Service should not care whether files are stored in:

* Local storage
* MinIO
* Amazon S3

Likewise, background jobs should not depend on a specific queue implementation.

Technology choices should remain replaceable whenever possible.

---

# 4.11 Consistency

Whenever possible, the platform should remain in a valid state.

The database and file storage represent two independent systems.

Spring transactions protect only the database.

Because the filesystem cannot participate in a database transaction, the project uses a **Compensation Pattern**.

Current behaviour:

```text
Store file

↓

Save entity

↓

Database fails

↓

Delete stored file

↓

Rethrow exception
```

This manual compensation prevents orphan files from remaining on disk.

---

# 4.12 Readability Over Cleverness

Code is written for humans first.

A slightly longer implementation is preferred if it is easier to understand.

The project intentionally avoids unnecessary abstractions or advanced language features that reduce readability.

Future contributors should be able to understand the code without requiring knowledge of hidden conventions.

---

# Summary

Every feature implemented in this project should respect these principles.

Before introducing a new class, dependency or technology, the following questions should always be considered:

* Does it solve a real problem?
* Does it reduce coupling?
* Does it increase cohesion?
* Is it easy to replace?
* Does it improve maintainability?
* Is the added complexity justified?

If the answer to these questions is "no", the simplest existing solution is usually the correct one.

# 6. Components

This section describes the responsibilities and interactions of every major component in the system.

Each component is documented independently to clearly define:

* Its purpose.
* Its responsibilities.
* Its dependencies.
* What it knows.
* What it deliberately does **not** know.

This separation makes the architecture easier to understand and reinforces the design principles presented earlier.

---

# 6.1 UploadController

## Purpose

`UploadController` is the entry point of the application.

It exposes the HTTP API used by clients to upload files and delegates the entire upload process to the service layer.

The controller acts exclusively as the bridge between the HTTP protocol and the application's business logic.

It contains no business rules.

---

## Responsibilities

The controller is responsible for:

* Receiving HTTP requests.
* Extracting request parameters.
* Passing the uploaded file to the service layer.
* Returning the response generated by the service.

It does **not** perform validation, storage or persistence.

---

## Current Implementation

```text id="e9x0yf"
Client

↓

POST /upload

↓

UploadController

↓

UploadService
```

The controller exposes a single endpoint:

```
POST /upload
```

The uploaded file is received as a multipart request parameter named:

```
file
```

The controller then delegates the request directly to:

```
uploadService.startUpload(file)
```

and returns the resulting `UploadResponse`.

---

## Dependencies

Current dependency:

```text id="l5ydpa"
UploadController

↓

UploadService
```

The controller depends only on the service layer.

It has no direct dependency on:

* repositories,
* storage,
* validators,
* databases,
* or the filesystem.

This significantly reduces coupling between the presentation layer and the rest of the application.

---

## Why Constructor Injection?

The controller receives its dependency through constructor injection.

```text id="l3t5pp"
UploadController

↓

UploadService
```

Constructor injection provides several advantages:

* Dependencies are explicit.
* The object is always created in a valid state.
* Dependencies become immutable (`final`).
* Unit testing becomes simpler.
* Spring can manage object creation automatically.

This is the preferred dependency injection strategy throughout the project.

---

## Responsibilities Deliberately Excluded

The controller intentionally avoids performing any business logic.

For example, it does **not**:

* Validate file size.
* Validate MIME types.
* Validate Magic Numbers.
* Store files.
* Save database records.
* Handle transactions.
* Decide where files are stored.
* Build HTTP error responses manually.

Those responsibilities belong to other layers.

---

## Input

Current input:

```text id="rzn1mw"
MultipartFile
```

Spring automatically converts the multipart HTTP request into a `MultipartFile` object before the controller method is executed.

The controller simply forwards this object to the service layer.

---

## Output

Current output:

```text id="v84n2l"
UploadResponse
```

The controller does not construct this object.

It simply returns the response produced by `UploadService`.

Spring automatically serializes the object into JSON before sending it back to the client.

---

## Error Handling

The controller currently performs no explicit exception handling.

If an exception occurs during the upload process, it propagates to the global exception handling mechanism (`GlobalExceptionHandler`).

This keeps the controller focused exclusively on request handling.

---

## Layer Boundaries

The controller represents the boundary between the outside world and the application.

```text id="pppnko"
HTTP

↓

Controller

↓

Business Logic
```

Everything related to HTTP ends here.

Beyond this point, the application operates independently of the transport protocol.

This design makes the business layer reusable and easier to test.

---

## Design Decisions

Several important architectural decisions can already be observed in this class:

* The controller has a single responsibility.
* It delegates all business logic.
* It depends only on the service layer.
* Dependencies are injected through the constructor.
* Responses are returned as DTOs rather than entities.
* Infrastructure concerns remain hidden behind the service layer.

Although the implementation is intentionally small, it follows the same architectural principles that will continue to apply as the platform grows.

# 6.2 UploadService

## Purpose

`UploadService` coordinates the complete upload workflow.

It represents the application's business layer and is responsible for orchestrating every step required to transform an incoming file into a persisted upload.

Unlike controllers, the service is completely independent of HTTP.

Its only concern is executing the business process.

---

## Responsibilities

The Upload Service is responsible for:

* Coordinating file validation.
* Creating the `Upload` entity.
* Delegating physical storage.
* Persisting metadata.
* Returning the upload result.
* Maintaining consistency between the database and the filesystem.

Rather than implementing low-level operations itself, the service delegates those responsibilities to specialized components.

---

## Dependencies

Current dependencies:

```text id="vl7h6q"
UploadService
    │
    ├──────────────► UploadRepository
    │
    ├──────────────► StorageService
    │
    ├──────────────► FileValidator
    │
    └──────────────► FileSignatureValidator
```

Each dependency has a single responsibility.

This allows the service to focus exclusively on coordinating the workflow.

---

## Upload Workflow

The current upload process is executed in the following order:

```text id="m1jtpb"
Receive MultipartFile
        │
        ▼
General validation
        │
        ▼
Magic Number validation
        │
        ▼
Create Upload entity
        │
        ▼
Store physical file
        │
        ▼
Persist Upload entity
        │
        ▼
Return UploadResponse
```

Each step depends on the successful completion of the previous one.

This ordering prevents invalid or inconsistent states from being created.

---

## Upload Identifier

The first operation performed by the service is generating a unique upload identifier.

```text id="j0w5oj"
UUID.randomUUID()
```

This identifier uniquely represents the uploaded file throughout the platform.

Later, the same identifier is reused as part of the storage path, ensuring a stable relationship between the database record and the stored object.

---

## Metadata Collection

Before creating the entity, the service extracts metadata from the uploaded file.

Current metadata includes:

* Original filename.
* Content type.
* File size.
* Upload date.
* Upload identifier.

This information becomes part of the `Upload` entity before persistence.

---

## Validation Phase

Validation is intentionally performed before any expensive operation.

Current validators:

```text id="wzftwx"
FileValidator

↓

FileSignatureValidator
```

If either validator fails, the workflow stops immediately.

At this stage:

* Nothing has been stored.
* Nothing has been persisted.
* No cleanup is required.

This follows the **Fail Fast** principle.

---

## Entity Creation

Once validation succeeds, the service creates the `Upload` entity.

At this point, the entity contains all business metadata except the storage path.

The storage path cannot exist yet because the file has not been written.

---

## Storage Phase

The service delegates physical storage to:

```text id="9q9nkz"
StorageService
```

Notice that the service never interacts with the filesystem directly.

Instead, it communicates only through the storage abstraction.

Current implementation:

```text id="d0hhcv"
StorageService

↓

LocalStorageService
```

Future implementations may replace Local Storage with object storage without modifying UploadService.

---

## Persistence Phase

Only after the file has been successfully stored does the service persist the entity.

```text id="v5tspk"
Storage completed

↓

UploadRepository.save()
```

This ordering guarantees that the database never references a file that does not exist.

---

## Compensation Strategy

The filesystem cannot participate in Spring transactions.

Therefore, if database persistence fails after the file has already been written, the service performs a manual compensation action.

Current behaviour:

```text id="4mzv0r"
Store file

↓

Database save fails

↓

deleteFile(storagePath)

↓

Rethrow exception
```

This prevents orphaned files from remaining on disk.

The database transaction is rolled back automatically, while the filesystem is restored manually.

---

## Transaction Management

The upload operation is annotated with:

```java id="4rrxpf"
@Transactional
```

This transaction protects database operations performed inside the method.

If a `RuntimeException` occurs, Spring automatically rolls back the database transaction.

It is important to note that the transaction **does not** include filesystem operations.

For this reason, manual compensation remains necessary.

---

## Returned Response

After successful persistence, the service constructs an `UploadResponse`.

The response currently contains:

* Upload status.
* Upload identifier.

Returning a DTO instead of the entity prevents exposing internal implementation details to API consumers.

---

## Responsibilities Deliberately Excluded

The service intentionally avoids implementing infrastructure details.

It does **not**:

* Read or write files directly.
* Execute SQL.
* Validate binary signatures itself.
* Handle HTTP requests.
* Build HTTP responses.
* Serialize JSON.

Each of these responsibilities belongs to another component.

---

## Design Decisions

Several architectural decisions are visible in this implementation:

* Business logic is centralized in the service layer.
* Infrastructure is accessed through abstractions.
* Validation is delegated to dedicated validators.
* Persistence occurs only after successful storage.
* Manual compensation maintains consistency.
* Database transactions protect persistent state.
* DTOs separate the API contract from domain entities.

Together, these decisions produce a service that is easy to understand, extend and test while remaining independent from the underlying infrastructure.

---

## Future Evolution

As the platform evolves, `UploadService` will remain responsible for coordinating the upload process.

However, additional steps will be introduced after persistence, such as:

```text id="hqv5lq"
Store file

↓

Save Upload

↓

Publish message to RabbitMQ

↓

Workers process the file asynchronously
```

The service will continue acting as the orchestrator of the upload workflow while delegating specialised tasks to other components.

# 6.3 StorageService

## Purpose

`StorageService` defines the contract for storing and deleting files.

It represents an abstraction over the underlying storage system, allowing the application to remain independent of any specific storage technology.

The business layer knows **what** operations can be performed, but not **how** those operations are implemented.

---

## Responsibilities

The interface defines two operations:

```java id="v4m8kn"
void storeFile(Upload upload, MultipartFile multipartFile);

void deleteFile(String storagePath);
```

These methods describe the capabilities expected from any storage implementation.

The interface contains no implementation logic.

---

## Why an Interface?

One of the primary architectural goals of the platform is to avoid coupling business logic to infrastructure.

Without an interface, the dependency graph would look like this:

```text id="a0h4dc"
UploadService

↓

LocalStorageService
```

This would tightly couple the service layer to the local filesystem.

Instead, the application uses an abstraction:

```text id="glnjci"
UploadService

↓

StorageService

↓

LocalStorageService
```

The service no longer depends on a concrete implementation.

Instead, it depends on a contract.

---

## Current Implementation

At the current stage of the project there is a single implementation:

```text id="7ovmkw"
StorageService

↓

LocalStorageService
```

This implementation stores files on the local filesystem.

The Upload Service is unaware of this detail.

---

## Future Implementations

One of the reasons for introducing the interface early is that additional implementations can later be added without modifying business logic.

Examples include:

```text id="qhf4cx"
StorageService
      │
      ├────────► LocalStorageService
      │
      ├────────► MinIOStorageService
      │
      ├────────► AmazonS3StorageService
      │
      └────────► AzureBlobStorageService
```

Each implementation satisfies the same contract.

Only Spring's dependency injection configuration determines which implementation is used.

---

## Why Does storeFile() Receive Upload?

Instead of receiving only the file itself, the method also receives the `Upload` entity.

```java id="tbjlwm"
storeFile(Upload upload, MultipartFile multipartFile)
```

This design allows the storage implementation to use metadata already generated by the business layer.

For example:

* Upload identifier.
* Upload date.
* Original filename.

The storage implementation can therefore generate deterministic storage paths without duplicating business logic.

It can also update the entity by assigning the generated storage path.

---

## Why Does deleteFile() Receive storagePath?

Deleting a file requires only its storage location.

```java id="xfpnf9"
deleteFile(String storagePath)
```

The storage implementation does not need access to the entire entity.

Passing only the required information keeps the interface small and focused.

---

## Responsibilities Deliberately Excluded

`StorageService` does **not** define responsibilities related to:

* Database persistence.
* HTTP communication.
* Validation.
* Metadata generation.
* Business rules.

Its only concern is physical object storage.

---

## Dependency Direction

The dependency structure is intentionally simple:

```text id="hdypxt"
UploadService

↓

StorageService

↓

Storage Implementation
```

This keeps infrastructure isolated from business logic.

The Upload Service remains unaware of:

* file paths,
* directories,
* object storage,
* buckets,
* cloud providers,
* filesystem APIs.

---

## Architectural Benefits

Introducing this interface provides several long-term advantages.

### Loose Coupling

Business logic is no longer tied to a particular storage implementation.

---

### Replaceability

Changing storage providers does not require changes to the service layer.

---

### Testability

During unit testing, a fake or mock implementation can easily replace the real storage system.

```text id="4ys4au"
UploadService

↓

FakeStorageService
```

No filesystem interaction is required.

---

### Scalability

As the platform evolves from local storage to distributed object storage, UploadService remains unchanged.

Only the injected implementation changes.

---

## Design Decisions

Several important architectural principles can already be observed:

* Dependency Inversion Principle.
* Programming to interfaces.
* Separation between business logic and infrastructure.
* Low coupling.
* High replaceability.
* Future-proof design.

Although the interface currently exposes only two methods, it establishes one of the most important extension points in the entire platform.

Future infrastructure improvements can be implemented behind this contract while preserving the stability of the business layer.

# 6.4 LocalStorageService

## Purpose

`LocalStorageService` is the current implementation of the `StorageService` interface.

Its responsibility is to persist uploaded files on the local filesystem.

Unlike `UploadService`, this component contains no business logic.

It is purely an infrastructure component responsible for interacting with the operating system.

---

## Responsibilities

The service is responsible for:

* Building the storage path.
* Creating missing directories.
* Writing files to disk.
* Deleting stored files.
* Assigning the generated storage path to the `Upload` entity.

It is **not** responsible for:

* Validating files.
* Persisting metadata.
* Handling HTTP requests.
* Managing transactions.

---

## Position in the Architecture

```text id="i5y6rp"
UploadService
        │
        ▼
StorageService
        │
        ▼
LocalStorageService
        │
        ▼
Operating System
        │
        ▼
Local File System
```

This is the lowest layer currently implemented inside the application.

It is the only component that interacts directly with the filesystem.

---

# Storage Path Generation

Before storing a file, the service generates a deterministic storage path.

Current structure:

```text id="r0m7bb"
year/month/uploadId.extension
```

Example:

```text id="1r3rwx"
2026/08/

↓

5b41d45e-9c59-46fa-bcef-dce62b8d7b44.jpg
```

Final path:

```text id="6dt1r4"
2026/08/5b41d45e-9c59-46fa-bcef-dce62b8d7b44.jpg
```

---

## Why This Structure?

Organising files by date provides several advantages.

### Prevents Huge Directories

Instead of storing thousands of files inside one directory:

```text id="ioklj7"
uploads/

10,000 files
```

the files are distributed:

```text id="7xmn1y"
uploads/

2026/
    01/
    02/
    03/
    ...
```

Directory sizes remain manageable.

---

### Predictable Organisation

The path can always be reconstructed from the entity metadata.

This makes debugging and manual inspection easier.

---

### Unique Filenames

Using the upload UUID as the filename guarantees uniqueness.

Two users uploading:

```text id="xfdj7s"
image.jpg
```

will never overwrite each other's files.

---

# Writing Files

The storage workflow is currently:

```text id="b77tyr"
Generate storage path

↓

Create directories

↓

Write file

↓

Assign storagePath
```

Only after the file has been written successfully is the generated storage path stored inside the `Upload` entity.

This guarantees that the entity never references a path that was not successfully created.

---

# Directory Creation

Before writing the file, the service ensures that the destination directory exists.

```text id="8nvcnm"
Files.createDirectories(...)
```

Unlike creating a single directory, this operation creates every missing parent directory automatically.

Example:

```text id="7lffhv"
uploads/

↓

uploads/2026/

↓

uploads/2026/08/
```

If the directories already exist, no error is produced.

---

# File Creation

Files are written using:

```text id="a97wq7"
StandardOpenOption.CREATE_NEW
```

This option guarantees that an existing file will never be overwritten accidentally.

If a file already exists at the destination path, Java throws an exception instead.

Although UUID collisions are practically impossible, this behaviour provides an additional layer of safety.

---

# Storage Path Assignment

After successfully storing the file, the service updates the entity.

```text id="9kmmdh"
upload.setStoragePath(storagePath)
```

This information is later persisted by the repository.

The responsibility for generating the storage path therefore belongs entirely to the storage implementation.

The business layer never constructs filesystem paths.

---

# File Deletion

The service also provides a deletion operation.

Current implementation:

```text id="h6h20r"
deleteFile(storagePath)
```

This method is primarily used as a compensation action when database persistence fails.

Current flow:

```text id="xufh9a"
Store file

↓

Database save fails

↓

Delete stored file
```

This keeps the filesystem consistent with the database.

---

# Exception Handling

Filesystem operations may fail for many reasons:

* Missing permissions.
* Full disk.
* Invalid paths.
* File locks.
* I/O failures.

Rather than exposing low-level `IOException`s to the rest of the application, the service converts them into a domain-specific exception.

```text id="gcytdq"
IOException

↓

StorageException
```

This hides infrastructure details from higher layers.

The business layer only needs to know that the storage operation failed.

It does not need to know how the underlying filesystem works.

---

# External Configuration

The storage root directory is not hardcoded.

Instead, it is obtained from:

```text id="tbd0sz"
StorageProperties
```

This allows different environments to use different storage locations without modifying application code.

Examples:

Development:

```text id="7j0tjc"
/uploads
```

Production:

```text id="dsmg9v"
/mnt/storage/uploads
```

The implementation remains unchanged.

---

# Responsibilities Deliberately Excluded

This class intentionally avoids responsibilities unrelated to storage.

It does **not**:

* Validate uploaded files.
* Generate upload identifiers.
* Save database entities.
* Perform HTTP operations.
* Decide upload status.
* Execute business rules.

Its only responsibility is interacting with the local filesystem.

---

# Current Limitation

The current implementation relies on the local filesystem.

While suitable for development and early deployment, this approach has important limitations.

Examples include:

* Files cannot be shared across multiple application instances.
* Horizontal scaling becomes difficult.
* Storage depends on the local machine.
* Files are lost if the machine is replaced without migration.

These limitations motivate the future migration to object storage.

---

# Future Evolution

This implementation is intentionally isolated behind the `StorageService` interface.

When the project evolves, it will be replaced by an object storage implementation such as MinIO.

The business layer will remain unchanged.

```text id="l1vxz9"
Today

UploadService

↓

LocalStorageService

────────────────────

Future

UploadService

↓

MinIOStorageService
```

This demonstrates one of the primary architectural goals of the project:

**business logic should remain independent of infrastructure technologies.**

# 6.5 UploadRepository

## Purpose

`UploadRepository` provides access to the persistence layer.

Its responsibility is to store and retrieve `Upload` entities from the database.

The repository acts as the boundary between the business layer and the persistence infrastructure.

Business logic never interacts directly with SQL.

---

## Responsibilities

The repository is responsible for:

* Persisting `Upload` entities.
* Retrieving uploads.
* Updating existing uploads.
* Deleting uploads.
* Querying upload data.

It contains **no business logic**.

---

## Position in the Architecture

```text id="ux4vcn"
UploadService
        │
        ▼
UploadRepository
        │
        ▼
Spring Data JPA
        │
        ▼
Hibernate
        │
        ▼
PostgreSQL
```

The service communicates only with the repository.

Everything below the repository is handled automatically by Spring Data JPA and Hibernate.

---

# Why Extend JpaRepository?

The repository extends:

```java id="cvjlwm"
JpaRepository<Upload, UUID>
```

This gives the project a complete persistence API without writing SQL manually.

Spring generates the implementation automatically at runtime.

No concrete class needs to be created.

---

# Generic Parameters

The repository declares two generic types.

```text id="7lm0sr"
JpaRepository<
    Upload,
    UUID
>
```

The first parameter indicates the entity managed by the repository.

```text id="jqjlwm"
Upload
```

The second parameter specifies the type of its primary key.

```text id="0fl2u5"
UUID
```

This allows Spring to generate type-safe CRUD operations.

---

# Available Operations

Although the interface appears almost empty, it already provides a large number of operations.

Examples include:

```java id="jlwmrd"
save()

findById()

findAll()

delete()

deleteById()

existsById()

count()
```

All these methods are inherited automatically.

No additional implementation is required.

---

# Current Usage

At the current stage of the project, the Upload Service uses the repository primarily for persistence.

Current workflow:

```text id="fquz9r"
Create Upload

↓

uploadRepository.save(upload)

↓

Persist entity
```

The returned entity contains the managed instance produced by JPA.

---

# Why Is the Repository an Interface?

Like many Spring Data components, the repository is declared as an interface rather than a class.

```text id="mjrkvc"
UploadRepository

↓

Interface
```

Spring automatically generates the implementation during application startup.

Developers focus on describing *what* operations are required rather than *how* they are implemented.

---

# Separation of Responsibilities

The repository has a single responsibility:

**database access.**

It deliberately avoids responsibilities such as:

* HTTP communication.
* File storage.
* Validation.
* Upload orchestration.
* Transaction management.
* Business decisions.

Those concerns belong to other layers.

---

# Why Not Write SQL?

At the current stage of the project, using Spring Data JPA provides several advantages.

### Less Boilerplate

Simple CRUD operations require no implementation.

---

### Type Safety

Compile-time checking prevents many common persistence errors.

---

### Readability

Business code remains focused on business behaviour rather than SQL statements.

---

### Productivity

Development is significantly faster while the domain model is still evolving.

As the platform grows, custom queries can be introduced only when necessary.

---

# Future Evolution

Although the repository is currently empty, it will gradually gain domain-specific queries.

Examples include:

```text id="vyo9xk"
Find uploads by status

Find uploads by date

Find uploads by checksum

Find uploads waiting for processing

Find failed uploads

Find expired uploads
```

Spring Data JPA allows many of these queries to be generated automatically from method names.

For more complex scenarios, custom JPQL or native SQL queries may be introduced.

---

# Why the Service Owns the Business Logic

The repository intentionally performs no decisions.

For example, it never decides:

* whether an upload is valid,
* when an upload should be stored,
* whether a file should be deleted,
* or whether processing should continue.

Those decisions belong exclusively to the service layer.

Keeping repositories free of business logic makes them easier to maintain, easier to test and reusable across different workflows.

---

# Design Decisions

Several architectural principles are visible in this component:

* Repository Pattern.
* Separation between persistence and business logic.
* Programming against abstractions.
* Spring Data code generation.
* Type-safe persistence through generics.
* Minimal boilerplate.

Although `UploadRepository` currently consists of only a single line of code, it provides the complete persistence API for the application's primary entity while keeping the service layer completely independent from SQL and database implementation details.

# 6.6 Upload Entity

## Purpose

`Upload` is the core domain entity of the platform.

It represents the metadata associated with an uploaded file.

The entity does **not** contain the file itself.

Instead, it stores the information required to identify, locate and manage the uploaded object throughout its lifecycle.

---

## Position in the Architecture

```text id="bqx3mv"
Client
    │
    ▼
UploadController
    │
    ▼
UploadService
    │
    ▼
Upload Entity
    │
    ▼
UploadRepository
    │
    ▼
Database
```

The `Upload` entity is the central object exchanged between the business layer and the persistence layer.

---

# Entity Overview

The entity currently stores:

| Field         | Purpose                         |
| ------------- | ------------------------------- |
| `uploadId`    | Unique identifier of the upload |
| `fileName`    | Original filename               |
| `storagePath` | Physical storage location       |
| `status`      | Current upload status           |
| `contentType` | MIME type                       |
| `size`        | File size in bytes              |
| `date`        | Upload date                     |

These fields describe the uploaded object without containing the object's binary data.

---

# Why Is the File Not Stored Here?

The uploaded file itself is **never** stored inside the entity.

Instead:

```text id="nifkzz"
Upload Entity

↓

Metadata
```

while

```text id="c0hrfw"
StorageService

↓

Binary File
```

This separation keeps the database lightweight and allows object storage systems to manage large files efficiently.

---

# Primary Key

The entity uses:

```java id="r8o35d"
@Id

private UUID uploadId;
```

Every upload receives a globally unique identifier.

Using UUIDs instead of sequential integers provides several advantages:

* Practically impossible collisions.
* Safe identifier generation without database coordination.
* Better suitability for distributed systems.
* Difficult to enumerate from the outside.

The same identifier is also reused when generating the storage path.

---

# Upload Status

The upload lifecycle is represented by:

```java id="sjlwm7"
UploadStatus
```

The value is stored using:

```java id="6cv9x8"
@Enumerated(EnumType.STRING)
```

Instead of saving numeric values:

```text id="2nby9h"
0

1

2
```

the database stores readable values:

```text id="a7wyui"
PENDING

PROCESSING

COMPLETED

FAILED
```

*(Current project state may only use `PENDING`, but the design already supports future states.)*

Using strings improves database readability and prevents accidental corruption if the enum order changes.

---

# File Metadata

The entity stores several pieces of metadata extracted during the upload process.

### Original Filename

```text id="fgkjlwm"
fileName
```

Preserves the filename provided by the client.

---

### MIME Type

```text id="jlwmnm"
contentType
```

Represents the declared content type of the uploaded file.

This value is useful for serving files correctly later.

---

### File Size

```text id="jlwm12"
size
```

Stores the file size in bytes.

This information can later be used for:

* validation,
* quotas,
* billing,
* analytics,
* monitoring.

---

### Upload Date

```text id="jlwm13"
date
```

Represents the date on which the upload was created.

This enables future features such as:

* filtering,
* retention policies,
* lifecycle management,
* reporting.

---

# Storage Path

One field deserves special attention:

```text id="jlwm14"
storagePath
```

Unlike the other metadata, this value is **not** known when the entity is created.

The upload workflow is currently:

```text id="jlwm15"
Create Upload

↓

Store File

↓

Generate storagePath

↓

Assign storagePath

↓

Persist Upload
```

The value is assigned by the storage implementation after the physical file has been successfully written.

This keeps the responsibility for storage path generation inside the storage layer.

---

# Constructors

The entity provides:

* a no-argument constructor,
* a parameterized constructor.

The empty constructor exists because JPA requires it when reconstructing entities from the database.

The parameterized constructor simplifies object creation inside the business layer.

---

# Getters and Setters

The entity currently exposes getters and setters for every field.

At this stage of the project, this approach keeps the implementation straightforward and easy to understand.

As the domain model evolves, some setters may become unnecessary in favour of more controlled state transitions.

---

# Current Lifecycle

The entity progresses through several stages during the upload process.

```text id="jlwm16"
Create Upload

↓

Validate file

↓

Store file

↓

Assign storagePath

↓

Persist entity

↓

Return response
```

Future versions of the platform will introduce additional lifecycle states after persistence.

---

# Future Evolution

The `Upload` entity will gradually become richer as new platform features are implemented.

Possible future fields include:

| Field             | Purpose                     |
| ----------------- | --------------------------- |
| `checksum`        | File integrity verification |
| `bucketName`      | Object storage bucket       |
| `duration`        | Video duration              |
| `resolution`      | Video dimensions            |
| `codec`           | Video codec                 |
| `thumbnailPath`   | Generated preview           |
| `virusScanStatus` | Antivirus result            |
| `uploadedBy`      | User ownership              |
| `createdAt`       | Timestamp                   |
| `updatedAt`       | Timestamp                   |
| `version`         | Optimistic locking          |

The current model intentionally remains minimal until those features become necessary.

---

# Design Decisions

Several important architectural decisions are reflected in this entity:

* The entity stores metadata rather than binary data.
* UUIDs provide globally unique identifiers.
* Upload state is represented by an enum.
* Storage information remains separate from business metadata.
* The entity is persistence-focused and contains almost no business logic.

This lightweight design keeps the domain model simple while providing a solid foundation for the future distributed architecture.

# 6.7 UploadResponse

## Purpose

`UploadResponse` is the Data Transfer Object (DTO) returned to the client after a successful upload request.

Its purpose is to expose only the information that the client needs while hiding the internal structure of the application's domain model.

The response represents the public API contract between the backend and its consumers.

---

## Position in the Architecture

```text id="zjq7tw"
UploadService
        │
        ▼
UploadResponse
        │
        ▼
UploadController
        │
        ▼
HTTP Response
        │
        ▼
Client
```

The service creates the response object, while Spring automatically serializes it into JSON.

---

# Why Use a DTO?

The application deliberately avoids returning the `Upload` entity directly.

Instead:

```text id="lwq5jh"
Upload Entity

↓

UploadResponse

↓

JSON
```

This introduces a separation between:

* the internal domain model,
* and the external API contract.

The client never communicates directly with JPA entities.

---

# Advantages of Using DTOs

Using DTOs provides several important benefits.

### Encapsulation

Internal implementation details remain hidden.

For example, the client does **not** receive:

* storage paths,
* database-specific fields,
* future internal metadata,
* persistence annotations.

---

### API Stability

The internal entity may evolve without breaking existing API consumers.

For example, new fields can be added to `Upload` without changing the response format.

---

### Security

Only explicitly selected information is exposed.

This prevents accidental leakage of internal data.

---

### Flexibility

Different endpoints can return different DTOs while sharing the same entity.

For example:

```text id="l2yz8x"
UploadResponse

UploadDetailsResponse

UploadListResponse

AdminUploadResponse
```

Each response can be tailored to its specific use case.

---

# Current Fields

The response currently contains two fields.

## Upload Status

```text id="q6s2yl"
status
```

Represents the current state of the upload.

At the current stage of the project this will normally be:

```text id="ntt1c5"
PENDING
```

In future versions, additional states such as `PROCESSING`, `COMPLETED` or `FAILED` may be returned.

---

## Upload Identifier

```text id="kp4s6w"
uploadId
```

The upload identifier uniquely identifies the uploaded file.

This identifier allows future client operations such as:

* checking processing status,
* downloading results,
* retrieving metadata,
* deleting uploads,
* querying progress.

Returning the identifier immediately is especially valuable once processing becomes asynchronous.

---

# Current JSON Representation

A successful upload currently produces a response similar to:

```json
{
  "status": "PENDING",
  "uploadId": "5b41d45e-9c59-46fa-bcef-dce62b8d7b44"
}
```

This response is intentionally compact.

The client receives only the information required to continue interacting with the upload.

---

# Immutability

The class currently exposes:

* a default constructor,
* a parameterized constructor,
* getters.

No setters are provided.

This makes the response object effectively immutable after creation.

Once constructed by the service layer, its contents cannot be modified.

Immutability reduces accidental state changes and makes DTOs safer to use.

---

# Current Lifecycle

The response object is created only after the upload workflow completes successfully.

```text id="bg3h9r"
Upload completed

↓

Create UploadResponse

↓

Return to Controller

↓

Serialize to JSON

↓

Send to Client
```

If an exception occurs before this point, the response is never created.

Instead, the global exception handling mechanism generates an error response.

---

# Future Evolution

As the platform evolves, this DTO may expose additional information.

Possible future fields include:

| Field                     | Purpose                     |
| ------------------------- | --------------------------- |
| `message`                 | Human-readable status       |
| `createdAt`               | Upload timestamp            |
| `processingUrl`           | Endpoint for polling status |
| `downloadUrl`             | Generated download link     |
| `estimatedProcessingTime` | Processing estimate         |

These additions can be introduced without affecting the internal entity.

---

# Design Decisions

Several architectural principles are demonstrated by this class:

* Separation between domain and API models.
* Minimal public contract.
* Encapsulation of internal state.
* Immutable response objects.
* Stable API design.

Although `UploadResponse` is intentionally small, it establishes an important boundary between the backend implementation and external API consumers.

This separation becomes increasingly valuable as the project grows and the internal domain model becomes more complex.

# 6.8 FileValidator

## Purpose

`FileValidator` performs the first stage of validation during the upload process.

Its responsibility is to verify that an uploaded file satisfies the application's general validation rules before any expensive operation is performed.

This validator focuses on **high-level properties** of the uploaded file.

It does **not** inspect the file contents.

---

## Position in the Architecture

```text id="j9rqxv"
UploadService
        │
        ▼
FileValidator
        │
        ▼
Validation Passed
        │
        ▼
Continue Upload
```

If validation fails, the upload process stops immediately.

---

# Responsibilities

The validator currently verifies:

* The file is not empty.
* The file size is within the configured limit.
* The declared MIME type is allowed.

If any validation fails, a `FileValidationException` is thrown.

---

# Validation Strategy

Validation is intentionally performed before:

* storing the file,
* writing to the database,
* creating directories,
* allocating unnecessary resources.

Current execution order:

```text id="ljlwm1"
Receive MultipartFile

↓

General Validation

↓

Magic Number Validation

↓

Store File
```

This follows the **Fail Fast** principle.

The application rejects invalid uploads as early as possible.

---

# Empty File Validation

The first validation ensures that the uploaded file actually contains data.

```text id="jlwm2"
file.isEmpty()
```

If the file contains no bytes, the upload is rejected immediately.

Storing empty files would waste storage space while providing no useful value.

---

# File Size Validation

The validator also checks the maximum permitted file size.

Rather than hardcoding a limit inside the class, the value is obtained from:

```text id="jlwm3"
StorageProperties
```

This allows different environments to configure different limits without modifying the application code.

Example:

```text id="jlwm4"
Development

50 MB

────────────

Production

500 MB
```

The validation logic remains unchanged.

---

# MIME Type Validation

The final validation checks whether the declared content type belongs to the list of supported formats.

Current implementation:

```text id="jlwm5"
AllowedContentTypes
```

The validator simply verifies membership inside the predefined collection.

Example:

```text id="jlwm6"
image/jpeg

✓ Allowed

────────────

application/pdf

✗ Rejected
```

This prevents clients from uploading unsupported file types.

---

# Why Isn't This Enough?

Although MIME type validation is useful, it is not sufficient on its own.

Clients can easily falsify the declared content type.

For example:

```text id="jlwm7"
virus.exe

↓

Renamed to

image.jpg

↓

Declared as

image/jpeg
```

For this reason, a second validation stage exists:

```text id="jlwm8"
FileSignatureValidator
```

which verifies the actual binary signature (Magic Numbers).

Together, both validators provide stronger protection.

---

# Exception Handling

Whenever a validation rule fails, the validator throws:

```text id="jlwm9"
FileValidationException
```

The validator does **not**:

* return error codes,
* log failures,
* build HTTP responses.

It simply reports that the uploaded file violates a business rule.

The HTTP response is generated later by the global exception handler.

---

# External Configuration

Maximum file size is read from application configuration through:

```text id="jlwm10"
StorageProperties
```

Allowed MIME types are stored separately inside:

```text id="jlwm11"
AllowedContentTypes
```

Separating configuration from validation logic makes the validator easier to maintain and extend.

---

# Responsibilities Deliberately Excluded

This validator intentionally avoids responsibilities such as:

* Reading file bytes.
* Inspecting Magic Numbers.
* Virus scanning.
* Image validation.
* Video validation.
* Storage.
* Persistence.

Each validation concern is implemented independently.

---

# Future Evolution

The current validator represents only the first validation stage.

Future specialised validators may include:

```text id="jlwm12"
VirusValidator

ImageDimensionValidator

VideoDurationValidator

VideoCodecValidator

ChecksumValidator

DuplicateFileValidator
```

Each validator will continue following the Single Responsibility Principle.

The Upload Service will simply orchestrate them in sequence.

---

# Design Decisions

Several architectural principles are demonstrated by this component:

* Single Responsibility Principle.
* Fail Fast validation.
* Configuration-driven limits.
* Separation between validation and business logic.
* Independent validation components.
* Exception-based error reporting.

By keeping general validation isolated from binary validation and future specialised validators, the platform remains easy to extend without turning a single class into a large collection of unrelated rules.

# 6.9 FileSignatureValidator

## Purpose

`FileSignatureValidator` performs binary validation of uploaded files.

Unlike `FileValidator`, which verifies general metadata, this validator inspects the actual contents of the uploaded file.

Its purpose is to ensure that the uploaded file truly matches its declared content type.

This provides significantly stronger protection against spoofed uploads.

---

## Position in the Architecture

```text id="b7p4tw"
UploadService
        │
        ▼
FileValidator
        │
        ▼
FileSignatureValidator
        │
        ▼
StorageService
```

The validator executes immediately after the general validation stage.

Only files that pass both validations are allowed to continue.

---

# Why Is This Validator Necessary?

Checking only the declared MIME type is insufficient.

A malicious client can easily send:

```text id="c8r6ym"
virus.exe

↓

Rename to

image.jpg

↓

Declare

image/jpeg
```

The server would receive a request that appears valid even though the file is not an image.

For this reason, the application verifies the file's binary signature.

---

# What Is a File Signature?

Most file formats begin with a unique sequence of bytes.

These bytes are commonly known as:

* Magic Numbers
* File Signatures
* File Headers

They uniquely identify many file formats regardless of filename or extension.

Example:

| Format | Signature (Hex)           |
| ------ | ------------------------- |
| PNG    | `89 50 4E 47 0D 0A 1A 0A` |
| JPEG   | `FF D8 FF`                |
| GIF87a | `47 49 46 38 37 61`       |
| GIF89a | `47 49 46 38 39 61`       |
| WEBP   | `52 49 46 46`             |

These values are significantly harder to fake than a filename or MIME type.

---

# Signature Registry

Supported signatures are stored inside:

```text id="q5y2nb"
ALLOWED_SIGNATURES
```

The registry maps:

```text id="1m8bpa"
Content-Type

↓

List of valid signatures
```

For example:

```text id="8x7vjn"
image/gif

↓

GIF87a

GIF89a
```

Using a map allows new file formats to be added with minimal changes to the validator itself.

---

# Validation Workflow

The validation process is currently:

```text id="5g7wlt"
Read declared Content-Type

↓

Retrieve expected signatures

↓

Read first bytes

↓

Compare signatures

↓

Accept or reject
```

Only the beginning of the file is inspected.

The remainder of the file is not read during this validation step.

---

# Reading the File

The validator opens the uploaded file using:

```java id="sqr8df"
try (InputStream inputStream = file.getInputStream())
```

A *try-with-resources* block is used so that the stream is always closed automatically.

Even if an exception occurs, Java guarantees that the underlying resource is released.

This prevents resource leaks.

---

# Reading Only the Header

Rather than reading the complete file, the validator reads only:

```text id="2j3lwb"
First 8 bytes
```

using:

```java id="jlwm81"
readNBytes(8)
```

This is sufficient because every currently supported signature fits within those first bytes.

Reading only a few bytes keeps validation extremely efficient, even for very large files.

---

# Signature Comparison

Each supported signature is compared against the beginning of the uploaded file.

Current process:

```text id="jlwm82"
Header

↓

Copy required bytes

↓

Arrays.equals()

↓

Match?
```

If any valid signature matches, validation succeeds immediately.

The validator returns without inspecting the remaining signatures.

---

# Multiple Valid Signatures

Some file formats have more than one valid signature.

For example:

```text id="jlwm83"
GIF87a

GIF89a
```

Instead of assuming a single signature, the validator stores a list of accepted values for each MIME type.

This design naturally supports formats with multiple valid headers.

---

# Unsupported Types

Before reading the file, the validator checks whether the declared content type exists in the registry.

If no signatures are registered, the upload is rejected immediately.

```text id="jlwm84"
Content-Type

↓

No registered signature

↓

Reject upload
```

This prevents unknown formats from reaching the storage layer.

---

# Exception Handling

Reading uploaded files may throw an `IOException`.

Rather than exposing low-level I/O details, the validator converts the exception into:

```text id="jlwm85"
FileValidationException
```

The higher layers therefore receive a business-oriented validation error rather than a filesystem exception.

---

# Responsibilities Deliberately Excluded

The validator intentionally avoids responsibilities such as:

* File size validation.
* MIME type validation.
* Virus scanning.
* Image decoding.
* Video inspection.
* Persistence.
* Storage.

Its only responsibility is verifying binary signatures.

---

# Performance Considerations

The validator is intentionally lightweight.

Current complexity:

```text id="jlwm86"
Read ≤ 8 bytes

↓

Compare small byte arrays
```

No temporary files are created.

No full file is loaded into memory.

Validation remains effectively constant time regardless of file size.

---

# Future Evolution

As additional file formats become supported, only the signature registry needs to grow.

Possible future additions include:

```text id="jlwm87"
PDF

MP4

MP3

ZIP

HEIC

AVI

MOV
```

The validation algorithm itself can remain unchanged.

This makes the implementation highly maintainable.

---

# Design Decisions

Several architectural principles are demonstrated by this validator:

* Single Responsibility Principle.
* Fail Fast validation.
* Binary verification instead of trusting client metadata.
* Efficient resource management through try-with-resources.
* Constant-time header validation.
* Configuration through a central signature registry.
* Exception translation from infrastructure to business errors.

Together with `FileValidator`, this component forms the first security layer of the upload pipeline, ensuring that only files whose declared type matches their actual binary contents are allowed into the system.

# 6.10 StorageProperties

## Purpose

`StorageProperties` centralises the application's storage-related configuration.

Instead of hardcoding infrastructure values throughout the codebase, the application loads them from external configuration files and exposes them as a strongly typed Java object.

This separates configuration from business logic.

---

## Position in the Architecture

```text id="m3q2za"
application.yml

        │

        ▼

StorageProperties

        │

        ├──────────────► LocalStorageService

        │

        └──────────────► FileValidator
```

Multiple components can reuse the same configuration without duplicating values.

---

# Configuration Binding

The class is annotated with:

```java id="xk9v3b"
@ConfigurationProperties(prefix = "storage")
```

Spring Boot automatically binds every property beginning with:

```text id="9q2wla"
storage.*
```

to this class.

For example:

```yaml id="d7j9hm"
storage:
  root-path: uploads
  max-file-size: 52428800
```

is automatically converted into:

```text id="r0d6wn"
StorageProperties

rootPath

maxFileSize
```

No manual parsing is required.

---

# Current Properties

The project currently defines two configuration values.

## rootPath

```text id="2m8tqp"
rootPath
```

Defines the base directory used for storing uploaded files.

Example:

```text id="6w4krc"
/uploads
```

The storage implementation appends its generated directory structure beneath this root.

---

## maxFileSize

```text id="q7znmv"
maxFileSize
```

Defines the maximum permitted upload size.

This value is consumed by `FileValidator` during the validation phase.

Keeping the limit configurable allows different environments to enforce different upload policies.

---

# Why External Configuration?

Hardcoding infrastructure values creates several problems.

For example:

```java id="jw5zrt"
"/uploads"

50_000_000
```

Changing either value would require modifying the source code and rebuilding the application.

Instead, configuration is externalised.

This allows behaviour to change without changing the implementation.

---

# Benefits

External configuration provides several important advantages.

### Environment Independence

Development:

```text id="l0t9qw"
/tmp/uploads
```

Production:

```text id="j5xvkm"
/mnt/storage/uploads
```

The application code remains identical.

---

### Single Source of Truth

Every component reads the same configuration object.

No duplicated constants exist across the codebase.

---

### Maintainability

Changing upload limits or storage locations requires only a configuration update.

No Java classes need modification.

---

### Type Safety

Unlike reading raw strings manually, Spring automatically converts configuration values into strongly typed Java fields.

For example:

```text id="p6m2dy"
String

↓

long

↓

boolean

↓

Duration
```

depending on the declared property type.

This reduces parsing errors and improves readability.

---

# Current Consumers

At the current stage of the project:

```text id="c1z9ur"
StorageProperties

        │

        ├────────► LocalStorageService

        │

        └────────► FileValidator
```

Both infrastructure and validation reuse the same configuration object.

This avoids duplicated configuration logic.

---

# Responsibilities Deliberately Excluded

`StorageProperties` is intentionally simple.

It does **not**:

* Validate configuration.
* Access the filesystem.
* Perform storage operations.
* Handle uploads.
* Contain business logic.

Its only responsibility is exposing configuration values.

---

# Future Evolution

As the platform evolves, additional storage configuration may be added.

Possible future properties include:

| Property             | Purpose                       |
| -------------------- | ----------------------------- |
| `bucketName`         | Object storage bucket         |
| `endpoint`           | MinIO or S3 endpoint          |
| `region`             | Cloud region                  |
| `accessKey`          | Storage credentials           |
| `secretKey`          | Storage credentials           |
| `multipartThreshold` | Multipart upload limit        |
| `temporaryDirectory` | Temporary processing location |

The same configuration mechanism can be reused without affecting existing components.

---

# Design Decisions

Several architectural principles are demonstrated by this component:

* Separation between configuration and implementation.
* Externalised infrastructure configuration.
* Strongly typed configuration binding.
* Single source of truth.
* Environment-independent application design.

Although the class is intentionally small, it plays an important role in keeping infrastructure concerns outside the business logic and making the application easier to configure across different deployment environments.

# 6.11 GlobalExceptionHandler

## Purpose

`GlobalExceptionHandler` centralises exception handling for the entire REST API.

Instead of every controller manually converting exceptions into HTTP responses, a single component is responsible for translating application exceptions into consistent JSON responses.

This keeps controllers focused exclusively on handling HTTP requests.

---

## Position in the Architecture

```text id="v1z6tm"
Controller

        │

Exception thrown

        │

        ▼

GlobalExceptionHandler

        │

        ▼

ErrorResponse

        │

        ▼

HTTP Response
```

Every exception matching one of the registered handlers follows this path automatically.

---

# Why Centralise Exception Handling?

Without a global exception handler, every controller would need to repeat code such as:

```text id="b4h9qj"
try

↓

catch

↓

build ResponseEntity

↓

return HTTP response
```

Across multiple controllers this quickly leads to duplicated code.

Instead:

```text id="m9t2kx"
Controller

↓

Throw exception

↓

GlobalExceptionHandler

↓

JSON response
```

Controllers remain clean and focused on their primary responsibility.

---

# @RestControllerAdvice

The class is annotated with:

```java id="g2x7lr"
@RestControllerAdvice
```

This annotation tells Spring Boot to:

* monitor exceptions thrown by controllers,
* intercept matching exceptions,
* invoke the appropriate handler method,
* automatically serialize the returned object as JSON.

The controllers themselves do not need to know this mechanism exists.

---

# Current Handler

At the current stage of the project, one exception is handled globally.

```java id="p5k3mn"
MethodArgumentNotValidException
```

This exception is thrown when request validation performed by Spring fails.

---

# Exception Mapping

The handler is registered using:

```java id="s8q1ve"
@ExceptionHandler(MethodArgumentNotValidException.class)
```

Whenever Spring throws this exception, the framework automatically invokes:

```text id="w0n5ch"
badRequestException(...)
```

No explicit calls are required.

---

# HTTP Status

The handler specifies:

```java id="h3m7xy"
@ResponseStatus(HttpStatus.BAD_REQUEST)
```

As a result, the client receives:

```text id="u2k9dz"
HTTP 400 Bad Request
```

This clearly indicates that the request itself was invalid.

---

# ErrorResponse

Rather than returning plain text, the handler returns a structured DTO.

Current response fields:

* HTTP status code.
* Error message.
* Timestamp.

Example:

```json id="x4p8wl"
{
  "status": 400,
  "message": "Invalid Request",
  "timestamp": "2026-08-05T18:42:15"
}
```

Returning structured JSON makes the API easier to consume programmatically.

---

# Why Return a DTO?

Using an `ErrorResponse` object provides several advantages.

### Consistency

Every error follows the same structure.

---

### Extensibility

Additional fields can later be added without changing controller code.

Examples:

* request path,
* error code,
* validation details,
* correlation ID,
* documentation link.

---

### Separation of Concerns

Controllers never build error responses directly.

The responsibility belongs entirely to the global handler.

---

# Current Limitation

The handler currently manages only one exception type.

The project already defines additional custom exceptions such as:

```text id="n7v2pk"
FileValidationException

StorageException
```

These are not yet mapped to HTTP responses.

Future versions of the handler will translate them into appropriate status codes.

---

# Future Evolution

As the platform grows, the global handler will become the single entry point for API error handling.

Possible mappings include:

| Exception                        | HTTP Status |
| -------------------------------- | ----------: |
| `FileValidationException`        |         400 |
| `StorageException`               |         500 |
| `EntityNotFoundException`        |         404 |
| `AccessDeniedException`          |         403 |
| `AuthenticationException`        |         401 |
| `MaxUploadSizeExceededException` |         413 |

This keeps error handling consistent across the entire application.

---

# Responsibilities Deliberately Excluded

The `GlobalExceptionHandler` intentionally does **not**:

* perform validation,
* log business events,
* retry failed operations,
* recover from infrastructure failures,
* contain business logic.

Its only responsibility is converting exceptions into HTTP responses.

---

# Design Decisions

Several architectural principles are demonstrated by this component:

* Centralised exception handling.
* Separation between business logic and HTTP concerns.
* Consistent API error format.
* Reusable error response model.
* Minimal controllers.

Although the current implementation is intentionally small, it establishes the foundation for a consistent error handling strategy across the entire platform. As more custom exceptions are introduced, this class will become the single place responsible for exposing meaningful and predictable error responses to API consumers.

# 6.12 FileValidationException

## Purpose

`FileValidationException` represents a business error indicating that an uploaded file failed one or more validation rules.

Rather than exposing low-level implementation details, the application communicates validation failures using a domain-specific exception.

This makes the intent of the error immediately clear throughout the application.

---

## Position in the Architecture

```text id="m7q2ha"
FileValidator

        │

        ▼

FileSignatureValidator

        │

Validation fails

        │

        ▼

FileValidationException

        │

        ▼

GlobalExceptionHandler

        │

        ▼

HTTP Response
```

The validator reports the failure.

Higher layers decide how to present it to the client.

---

# Why Create a Custom Exception?

Java already provides generic exceptions such as:

```text id="v9x4pw"
IllegalArgumentException

RuntimeException

Exception
```

However, these exceptions do not explain *what actually happened*.

By introducing:

```text id="z4m6kc"
FileValidationException
```

the application can distinguish file validation errors from every other type of failure.

The exception itself becomes part of the domain language.

---

# What Does It Represent?

This exception indicates that the uploaded file violates one of the application's business rules.

Examples include:

* Empty file.
* File too large.
* Unsupported MIME type.
* Invalid Magic Number.
* Corrupted upload.

These are expected failures caused by invalid client input rather than programming mistakes.

---

# Why Extend RuntimeException?

The class extends:

```java id="b2r8tw"
RuntimeException
```

instead of:

```java id="f5v1nj"
Exception
```

This makes it an **unchecked exception**.

As a result:

* methods do not need to declare `throws`,
* callers are not forced to catch it,
* validation failures naturally interrupt the upload flow.

The application simply stops processing and lets the exception propagate to the global exception handler.

---

# Constructors

The class provides two constructors.

## Message Constructor

```java id="y3p6mz"
FileValidationException(String message)
```

Used when only a human-readable explanation is required.

Example:

```text id="j6w8qh"
"File is empty."

"File is too big."

"Unsupported content type."
```

---

## Message and Cause Constructor

```java id="l8n2vk"
FileValidationException(String message, Throwable cause)
```

Used when another exception caused the validation failure.

For example:

```text id="d0m5ry"
IOException

↓

FileValidationException
```

Passing the original exception preserves the complete stack trace while exposing a business-oriented exception to the rest of the application.

---

# Exception Translation

One of the key ideas behind this class is **exception translation**.

Instead of allowing infrastructure exceptions to leak into higher layers:

```text id="p4h9zt"
IOException

↓

Controller
```

the application converts them into:

```text id="w1x6lu"
FileValidationException

↓

Controller
```

The business layer no longer depends on implementation-specific exceptions.

---

# Current Usage

The exception is currently thrown from:

```text id="n5q7bc"
FileValidator

FileSignatureValidator
```

Both validators report failures using the same domain-specific exception.

This allows the rest of the application to treat every validation failure consistently.

---

# Future Usage

As new validation components are introduced, they can continue using the same exception.

Examples:

```text id="h8r2qy"
VirusValidator

ImageDimensionValidator

VideoDurationValidator

DuplicateFileValidator
```

All validation failures can be represented by a single exception type.

---

# Responsibilities Deliberately Excluded

`FileValidationException` intentionally contains:

* no validation logic,
* no HTTP logic,
* no logging,
* no recovery behaviour.

Its only purpose is to describe a specific category of business failure.

---

# Design Decisions

Several architectural principles are reflected in this class:

* Domain-specific exceptions.
* Exception translation.
* Unchecked exceptions for business flow interruption.
* Clear separation between infrastructure errors and validation errors.
* Reusable validation error type.

Although the implementation consists of only two constructors, this class establishes a common language for reporting validation failures throughout the upload pipeline. By using a dedicated exception instead of generic Java exceptions, the application becomes easier to understand, easier to maintain and easier to extend as additional validation rules are introduced.

# 6.13 StorageException

## Purpose

`StorageException` represents failures that occur while interacting with the file storage system.

Unlike `FileValidationException`, which indicates that the uploaded file is invalid, `StorageException` indicates that the file could not be stored, read or deleted due to an infrastructure problem.

This distinction separates **business errors** from **infrastructure failures**.

---

## Position in the Architecture

```text id="q8m4tz"
UploadService

        │

        ▼

StorageService

        │

        ▼

LocalStorageService

        │

Filesystem error

        │

        ▼

StorageException

        │

        ▼

GlobalExceptionHandler
```

The storage layer reports the failure.

Higher layers decide how to expose it to the client.

---

# What Does It Represent?

`StorageException` indicates that an operation involving physical storage could not be completed.

Typical examples include:

* Unable to create directories.
* Unable to write the uploaded file.
* Unable to delete a stored file.
* Missing filesystem permissions.
* Disk full.
* I/O failures.

These failures originate from the storage infrastructure rather than from invalid client input.

---

# Why Create a Custom Exception?

Java already provides exceptions such as:

```text id="h3n8pq"
IOException

FileNotFoundException

AccessDeniedException
```

However, exposing these exceptions directly would tightly couple the rest of the application to filesystem implementation details.

Instead, the storage layer translates them into:

```text id="m6w1yd"
StorageException
```

The business layer only needs to know that the storage operation failed.

It does not need to know how the underlying filesystem works.

---

# Why Extend RuntimeException?

The class extends:

```java id="y2q7kn"
RuntimeException
```

making it an unchecked exception.

Storage failures are considered unrecoverable within the current upload request.

When storage fails, continuing the upload process would leave the system in an inconsistent state.

Allowing the exception to propagate naturally immediately aborts the current operation.

---

# Constructors

The class provides two constructors.

## Message Constructor

```java id="v9c4lr"
StorageException(String message)
```

Used when only a business-oriented error message is required.

Example:

```text id="u6m5pt"
"Failed to store file"

"Failed to delete file"
```

---

## Message and Cause Constructor

```java id="t8r3bx"
StorageException(String message, Throwable cause)
```

Used when another exception triggered the storage failure.

Typical flow:

```text id="g5x9kv"
IOException

↓

StorageException
```

Passing the original exception preserves the complete stack trace for debugging while presenting a cleaner exception to higher layers.

---

# Exception Translation

`StorageException` is an example of **exception translation**.

Instead of allowing low-level exceptions to propagate:

```text id="b2k7ny"
IOException

↓

UploadService
```

the storage layer exposes:

```text id="x4d8qw"
StorageException

↓

UploadService
```

The service layer therefore depends only on domain-specific exceptions rather than Java's filesystem APIs.

---

# Current Usage

At the current stage of the project, `StorageException` is thrown by:

```text id="w7p1rz"
LocalStorageService

↓

storeFile()

deleteFile()
```

Both methods translate any `IOException` into a `StorageException`.

---

# Relationship with Transactions

One important design decision in the current implementation is that filesystem operations are **not transactional**.

For example:

```text id="l9v6mf"
Store file

↓

Save database entity

↓

Database failure
```

Spring can roll back the database transaction, but it cannot automatically remove the physical file.

For this reason, `UploadService` performs a manual compensation:

```text id="j1q8tw"
StorageException?

↓

deleteFile(storagePath)

↓

Re-throw exception
```

This keeps the filesystem and database consistent.

---

# Future Evolution

As the project evolves, the implementation behind `StorageService` will change.

Current:

```text id="k2x4ns"
LocalStorageService
```

Future:

```text id="m7y3df"
MinIOStorageService

S3StorageService
```

Those implementations will continue throwing the same `StorageException`.

The service layer will remain completely independent of the storage technology.

---

# Responsibilities Deliberately Excluded

`StorageException` intentionally contains:

* no storage logic,
* no retry mechanism,
* no logging,
* no HTTP behaviour,
* no recovery strategy.

Its only purpose is to represent infrastructure failures related to storage.

---

# Design Decisions

Several architectural principles are demonstrated by this class:

* Exception translation.
* Separation between infrastructure and business logic.
* Unchecked exceptions for unrecoverable request failures.
* Storage abstraction independent of implementation.
* Reusable exception across multiple storage providers.

Although the class itself is very small, it forms an important part of the application's architecture. By exposing a storage-specific exception instead of low-level filesystem exceptions, the rest of the application remains independent from the underlying storage technology and is prepared for future migration from local storage to object storage services such as MinIO or Amazon S3.

# 6.14 ErrorResponse

## Purpose

`ErrorResponse` is the standard Data Transfer Object (DTO) used to represent API errors.

Whenever an exception is translated into an HTTP response, the client receives an instance of this class serialized as JSON.

This provides a consistent and predictable error format across the entire API.

---

## Position in the Architecture

```text id="q8m5zx"
Exception

        │

        ▼

GlobalExceptionHandler

        │

        ▼

ErrorResponse

        │

        ▼

JSON

        │

        ▼

Client
```

The class acts as the public representation of an application error.

---

# Why Use an Error DTO?

Returning plain text such as:

```text id="b4y7lp"
"Something went wrong."
```

provides very little information to the client.

Instead, a structured response allows applications to process errors programmatically.

Current example:

```json id="v9q1rh"
{
    "status": 400,
    "message": "Invalid Request",
    "time": "2026-08-05T20:30:14"
}
```

Every error follows the same format regardless of where it originated.

---

# Current Fields

The DTO currently contains three pieces of information.

## HTTP Status

```text id="k6r4tm"
status
```

Represents the HTTP status code returned to the client.

Examples:

```text id="j2p8sv"
200

400

404

500
```

The numeric value makes the response easy to interpret both for humans and for client applications.

---

## Message

```text id="y5w3kn"
message
```

Contains a short description of the error.

The message explains *what happened* without exposing internal implementation details.

Examples:

```text id="c8z1mf"
Invalid Request

File is too big.

Unsupported content type.
```

---

## Timestamp

```text id="t7n4qx"
time
```

Stores the instant at which the error response was created.

Including timestamps is useful for:

* debugging,
* correlating logs,
* monitoring,
* support investigations.

---

# Serialization

Spring Boot automatically converts the DTO into JSON.

The controller never performs manual serialization.

Current flow:

```text id="g3p6ld"
Throw exception

↓

GlobalExceptionHandler

↓

Create ErrorResponse

↓

Automatic JSON serialization

↓

HTTP response
```

---

# Why Separate ErrorResponse from Exceptions?

Exceptions describe problems **inside** the application.

DTOs describe information **sent outside** the application.

Example:

```text id="r8x5jm"
StorageException

↓

ErrorResponse
```

The client never receives the exception object itself.

Instead, it receives a simplified and stable API contract.

This separation prevents internal implementation details from leaking outside the application.

---

# Current Usage

At the current stage of the project, `ErrorResponse` is created by:

```text id="m4k2ny"
GlobalExceptionHandler
```

Controllers never instantiate this class directly.

This centralises the application's error formatting in a single place.

---

# Future Evolution

As the API grows, additional fields may be introduced.

Possible additions include:

| Field           | Purpose                             |
| --------------- | ----------------------------------- |
| `path`          | Request URI                         |
| `errorCode`     | Stable application error identifier |
| `details`       | Validation errors                   |
| `requestId`     | Correlation identifier              |
| `documentation` | Link to API documentation           |
| `traceId`       | Distributed tracing identifier      |

The DTO is intentionally simple today while remaining easy to extend later.

---

# Mutability

Unlike `UploadResponse`, this DTO currently exposes both getters and setters.

This allows Spring or other components to construct and modify instances if required.

For the current stage of the project, this keeps the implementation straightforward.

As the application evolves, the class could become immutable if desired.

---

# Design Decisions

Several architectural principles are demonstrated by this class:

* Consistent API error format.
* Separation between internal exceptions and external responses.
* Dedicated DTOs for API communication.
* Automatic JSON serialization.
* Easily extensible response model.

Although the class only contains three fields, it establishes the foundation for a professional error handling strategy. Every future API error can be represented using the same predictable structure, making the backend easier to consume, debug and maintain.

# 6.15 UploadResponse

## Purpose

`UploadResponse` is the Data Transfer Object (DTO) returned to the client after a successful upload request.

Its purpose is to expose only the information required by the client while hiding the application's internal domain model.

Rather than returning the `Upload` entity directly, the API returns a simplified representation specifically designed for external consumers.

---

## Position in the Architecture

```text
Client

    │

    ▼

UploadController

    │

    ▼

UploadService

    │

    ▼

UploadResponse

    │

    ▼

JSON Response
```

The service creates the response object and Spring Boot automatically serializes it into JSON.

---

# Why Use a DTO?

Returning the JPA entity directly would expose implementation details that belong to the backend.

Instead, the application transforms the domain model into a response designed for the API.

```text
Upload Entity

↓

UploadResponse

↓

JSON
```

This creates a clear separation between the internal model and the public API contract.

---

# Current Fields

The response currently contains two fields.

## Upload Status

```text
status
```

Represents the current state of the upload.

At the current stage of the project, newly uploaded files always return:

```text
PENDING
```

In future versions this value will evolve as asynchronous processing is introduced.

Possible future states include:

* PROCESSING
* COMPLETED
* FAILED

---

## Upload Identifier

```text
uploadId
```

A globally unique identifier assigned when the upload begins.

The client can later use this identifier to:

* query upload status,
* retrieve metadata,
* download processed files,
* cancel processing,
* inspect processing history.

Returning the identifier immediately allows asynchronous workflows without requiring the client to wait for processing to finish.

---

# Current JSON Representation

Example response:

```json
{
    "status": "PENDING",
    "uploadId": "0a7bdf8d-96fb-49db-b29b-8b3d6cf6d19d"
}
```

The response is intentionally minimal.

Only information immediately useful to the client is exposed.

---

# Immutability

The class currently provides:

* a default constructor,
* a parameterized constructor,
* getters.

No setters are exposed.

This makes the object effectively immutable after construction.

Once created by the service layer, its state cannot be modified accidentally.

---

# Lifecycle

The response object is only created after the upload has completed successfully.

```text
Validate file

↓

Store file

↓

Persist Upload

↓

Create UploadResponse

↓

Return to client
```

If any step fails, an exception is thrown and the response is never created.

Instead, the request is handled by the global exception handler.

---

# Future Evolution

As the platform evolves, additional fields may become useful.

Possible additions include:

| Field                 | Purpose                        |
| --------------------- | ------------------------------ |
| `createdAt`           | Upload creation timestamp      |
| `processingUrl`       | Endpoint for polling progress  |
| `downloadUrl`         | Download link after processing |
| `checksum`            | SHA-256 identifier             |
| `estimatedCompletion` | Estimated processing time      |

The DTO can evolve independently of the `Upload` entity.

---

# Design Decisions

This class demonstrates several architectural principles:

* Dedicated DTOs for API communication.
* Separation between persistence and presentation models.
* Minimal public API contract.
* Immutable response objects.
* Stable interface for API consumers.

Although the class is intentionally small, it establishes an important architectural boundary between the application's internal implementation and the external REST API.
