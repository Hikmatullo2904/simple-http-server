# 🧩 My HTTP Server

A clean, minimal, and fully custom HTTP server written from scratch in Java 21.
It supports both regular HTTP request handling and WebSocket communication — built layer by layer, not using any existing frameworks.
The goal is to understand how real web servers work under the hood and design them in a clean, scalable, and modern way.

---
## 🚀 Features

- HTTP/1.1 implementation (request parsing, response generation, connection keep-alive).

- WebSocket support with frame parsing, handshake, and session management.

- Graceful shutdown – all WebSocket sessions and executors close properly when the server stops.

- Virtual Threads (Java 21) – highly efficient concurrency model with thousands of connections on minimal resources.

- Logging using slf4j + logback.

- Modular design – easy to extend with new protocols or handlers.

- Unit tests for:

  - HTTP request parsing

  - WebSocket frame parsing

  - Version detection and validation

  - Header parsing edge cases

---

🧠 Why I Built This

I use spring boot everyday. I am goot at it. But I always wanted to know how requests are coming, routing happens
and other networking related things. Then I started implementing webserver. I researched, watched videos, read networking books and used AI extendively.
This project is my hands-on way to explore:

- low-level networking in Java,

- request/response lifecycles,

- concurrency models (platform threads vs virtual threads),

- and clean architecture in systems programming.

---

🧱 Project Structure
```
src/
 ├─ main/java/uz/hikmatullo/httpserver/
 │   ├─ runtime/          → Core server runtime (listener, connection handler, shutdown logic) and thread exceturor holder
 │   ├─ http/             → HTTP parser, request/response models
 │   ├─ websocket/        → WebSocket handshake, session, frame IO
 │   ├─ util/             → Helper utilities, logging, etc.
 │   └─ exceptions        → exception classes
 └─ test/java/...         → all test cases
 ```
---

## ⚙️ How to Run
### 1. Requirements

Java 21+ (Eclipse Temurin 21 or Amazon Corretto 21 recommended)

Maven 3.9+

### 2. Build & Run
```
mvn clean package
java -jar target/my-http-server-1.0-SNAPSHOT.jar
```

The server will start listening on `localhost:8080` by default.

---

## 🧵 Concurrency Model

This version uses Java 21 virtual threads to handle connections:

- Each incoming socket connection runs on a lightweight virtual thread.

- Thousands of concurrent requests can be processed without exhausting OS threads.

- The design is simple, blocking-style, and highly readable — no complicated async APIs.

---

## 🧩 WebSocket Lifecycle

- Handshake upgrade from HTTP → WebSocket

- Frame reading/writing handled by WebSocketFrameReader and WebSocketFrameWriter

- Each client connection managed by WebSocketSession

- All active sessions tracked by WebSocketSessionManager

- Graceful close handled when:

  - the server stops

  - a client disconnects

  - an error occurs during frame read/write.
---

## 🔒 Graceful Shutdown

When you stop the server (Ctrl + C or IDE stop button):

- `WebSocketSessionManager.closeAll()` gracefully closes active sessions.

- `ExecutorsHolder.shutdownAll()` terminates all thread pools.

- Logs confirm every session cleanup and resource release.

---

## 🧪 Tests

Run all tests:
```bash
mvn test
```

The test suite covers:

- HTTP request parsing (methods, versions, headers)

- WebSocket frame encoding/decoding

- Connection closure behavior

- Edge cases in header and payload parsing

## 💡 Future Plans

- Implement non-blocking mode (NIO) for comparison

- Add HTTP/2 support (experimental)

- Benchmark blocking vs virtual-thread vs non-blocking servers

- Create Framework and use this server in it

## 👨‍💻 Author

Hikmatullo Anvarbekov
Software Engineer • CS lover
