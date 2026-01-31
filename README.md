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
- Entry point:org.example.client.DSMSClientMain

### Front-End
- Exposes SOAP endpoints to clients
- Communicates with the Sequencer via UDP
- Entry point:org.example.front_end.FrontEndInterfaceImpl

### Sequencer
- Orders all incoming requests
- Broadcasts ordered requests to replicas using UDP
- Entry point:org.example.sequencer.Sequencer

### Replica Managers
- Receive ordered requests
- Forward them to local replica services
- One manager per replica (Replica1–Replica4)

### Replicas
- Execute share market operations
- Each replica publishes its own SOAP service
- Designed to tolerate failures and inconsistencies

---

## ⚙️ Technologies Used

- Java
- Multithreading & Concurrency
- UDP Sockets
- SOAP (JAX-WS)
- Gradle
- Distributed System Design Patterns

---

## 🚀 How to Run (Local Setup)

> ⚠️ **Important**  
> Update IP addresses to `localhost` before running.

File to update:DSD-Final-Project/src/main/java/org/example/sequencer/Config.java
-Replace any hardcoded LAN IPs with: localhost or 127.0.0.1

### Recommended Startup Order (each in a separate terminal):

1. Sequencer  
2. Front-End  
3. Replica SOAP Publishers (all 4)  
4. Replica Managers (all 4)  
5. Client Application  

You can run each component directly from IntelliJ
by right-clicking the `main` class → **Run**.

---

## 🧠 Distributed Concepts Demonstrated

- Client–Server Architecture
- Process Replication
- Fault Tolerance
- Request Sequencing
- Concurrency Control
- UDP-based Communication
- Service-Oriented Architecture (SOAP)

---

## 📈 Possible Enhancements

- Replace SOAP with REST or gRPC
- Containerize services using Docker
- Deploy replicas using Kubernetes
- Add automated failure detection & recovery
- Introduce consensus mechanisms (e.g., Raft)

---

## 👤 Author

**Yash Ajmeri**  
Master’s in Applied Computer Science  
Concordia University


