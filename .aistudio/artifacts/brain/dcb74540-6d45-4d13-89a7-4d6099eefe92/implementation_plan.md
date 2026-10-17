# Python Backend PRD & Implementation Plan

This document outlines the requirements and architectural plan for a Python-based backend service. The primary goal is to provide a reliable, high-performance API for the Android movie application by proxying and cleaning data from official streaming backends.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed based on your requirements:
> - **Core Objective**: Proxy/Scraper for official APIs (not a standalone CMS).
> - **Tech Stack**: Python with FastAPI (Recommended for its native async support and performance).
> - **Deployment**: Render (using the Web Service tier).

- **Decision 1**: **FastAPI vs Flask**. I have selected **FastAPI** as the primary recommendation because it handles asynchronous network requests (I/O) much more efficiently than Flask, which is critical when proxying multiple external API calls.
- **Decision 2**: **Signature Logic**. The backend will centralize the `x-tr-signature` cryptographic logic, removing the need for the Android app to handle complex header generation.

---

### 1. Overview & Core Concept

- **What It Does**: A "Middleware" service that sits between the Android app and official movie servers. It intercepts requests, attaches required security headers, fetches data, cleans/flattens the JSON, extracts real streaming links (bypassing traps), and returns a simplified response to the mobile client.
- **Target Audience**: Developers and users of the Genga World / MovieBox Android client.
- **Key Value**: 
    - **Stability**: If official API endpoints change, only the backend needs an update, not the app.
    - **Performance**: Heavy data transformation and extraction happen on the server, not the user's phone.
    - **Security**: Official API keys and signature algorithms are hidden behind the server.

---

### 2. Technical Architecture & Data Strategy

The system follows a stateless proxy architecture optimized for speed and reliability.

#### System Architecture Diagram

```box
┌─────────────────────────┐      ┌─────────────────────────────┐
│      Android App        │      │       Python Backend        │
│   (Kotlin / Compose)    │◄────▶│      (FastAPI @ Render)     │
└─────────────────────────┘      └──────────────┬──────────────┘
                                                │
                                                ▼
                                 ┌─────────────────────────────┐
                                 │      Official Movie API     │
                                 │  (api6.aoneroom.com, etc.)  │
                                 └─────────────────────────────┘
```

#### Core Endpoints

| Endpoint | Method | Description |
|:---|:---|:---|
| `/trending` | GET | Returns a flat list of trending movies and shows. |
| `/search` | GET | Query parameters for universal search across all categories. |
| `/detail/{id}` | GET | Returns full metadata, cast, and structured episode/season lists. |
| `/stream/{id}` | GET | Resolves real playback URLs, bypassing dummy traps and providing quality options. |
| `/health` | GET | Health check for Render deployment monitoring. |

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Server-Side Signature Generation**
    - *Chosen Approach*: Backend generates the `x-tr-signature` for every request.
    - *Why*: The signature algorithm involves a specific timestamp and MD5/HMAC sequence. Centralizing this prevents "Signature Mismatch" errors across different app versions.
    - *Alternatives Considered*: Client-side generation (currently in app), but this is harder to maintain.

- **Decision 2: Response Flattening**
    - *Chosen Approach*: The backend will map complex nested structures (e.g., `data.list[0].items[0].subject`) into a simple list of objects.
    - *Why*: Reduces mobile data usage and makes the Android `LazyColumn` implementations significantly simpler and faster.

- **Decision 3: Render Deployment Strategy**
    - *Chosen Approach*: Use a `Dockerfile` for deployment on Render.
    - *Why*: Ensures a consistent environment (Python version, dependencies like `cryptography` and `httpx`) between local development and production.

---

### 4. Implementation Steps

1.  **Project Bootstrap**: Setup FastAPI with `httpx` for async HTTP requests and `pydantic` for data validation.
2.  **Signature Module**: Port the Java/Kotlin signature logic (`MD5`, `HMAC-MD5`, `Base64`) to a Python utility module.
3.  **Proxy Routes**: Implement the proxy handlers that forward requests with the correctly formatted `X-Client-Info` and `x-tr-signature` headers.
4.  **Extraction Logic**: Implement the "Dummy Video Bypass" (urlprefix decoding) and UGC recovery logic.
5.  **Deployment**: Create the `render.yaml` or `Dockerfile` for one-click deployment to Render.
