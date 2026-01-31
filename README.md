# Distributed Share Market System (DSMS)

A Java-based distributed system designed to simulate a share market platform using
replication, fault tolerance, and ordered request processing.

This project was developed as part of a Distributed Systems course and focuses on
core concepts such as client–server communication, process replication, concurrency,
and failure handling.

---

## 📌 System Overview

The system follows a **replicated architecture** with:
- A **Front-End** that receives client requests
- A **Sequencer** that orders requests using UDP
- **Multiple Replicas (4)** to ensure fault tolerance
- **Replica Managers** to manage execution and failure detection
- **SOAP (JAX-WS)** services for business logic
- A **Client application** to interact with the system

---

## 🧩 Core Components

### Client
- Sends requests to the Front-End
- Entry point:
