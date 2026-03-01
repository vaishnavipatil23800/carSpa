# 🟡 SERVICE 3 — API Gateway

**Role:** Single entry point for ALL frontend requests.
Validates JWT tokens and routes requests to the correct microservice.

> ⚠️ **Prerequisites:**
> - Service 1 (Eureka) running on port 8761
> - Service 2 (User Service) running on port 8081
> - No other service is required right now — gateway handles 503 gracefully

---

## 🏗️ How the Gateway Works

```
Frontend (React)
      │
      ▼ All requests go through port 8080
┌─────────────────────────────────────┐
│           API GATEWAY               │
│                                     │
│  1. Is this a public path?          │
│     → YES: forward immediately      │
│     → NO:  check Authorization      │
│                                     │
│  2. Is the Bearer token valid?      │
│     → NO:  return 401               │
│     → YES: inject X-User headers    │
│            and forward the request  │
└────────────┬────────────────────────┘
             │
    ┌────────┴──────────┐
    ▼                   ▼
user-service      booking-service
(8081)            (8082) etc...
```

**After the gateway validates a token, it adds these headers to the forwarded request:**
```
X-User-Email:  vaishnavi@test.com
X-User-Id:     1
X-User-Role:   ROLE_USER
X-User-Name:   Vaishnavi Patil
```

Downstream services use these headers — they never need to validate JWT themselves.

---

## ✅ STEP 1 — Open in IntelliJ

1. Open **IntelliJ IDEA**
2. **File → Open** → select the `03-api-gateway` folder
3. Wait for Maven import to finish

---

## ✅ STEP 2 — Check application.yml

Open `src/main/resources/application.yml`

Make sure the JWT secret matches your user-service exactly:
```yaml
jwt:
  secret: carspa-local-dev-secret-key-min-32-chars-change-in-prod
```

Both services **must use the same secret** — the gateway validates tokens,
user-service issues them. They must agree on the signing key.

---

## ✅ STEP 3 — Run the Service

**Option A — IntelliJ:**
1. Open `ApiGatewayApplication.java`
2. Click the green ▶️ button

**Option B — Command Prompt:**
```cmd
cd 03-api-gateway
mvn spring-boot:run
```

**Wait for this in the console:**
```
Started ApiGatewayApplication in X seconds
```

**Verify with Eureka:**
→ Open `http://localhost:8761`
→ You should now see both `USER-SERVICE` and `API-GATEWAY` listed

---

## ✅ STEP 4 — Run the Tests

```cmd
mvn test
```

`JwtUtilTest` runs without needing Eureka or MySQL — 6 unit tests.

---

## ✅ STEP 5 — Test With Postman

From now on, use **port 8080 (gateway)** instead of direct service ports.
The gateway is the only port your frontend should ever call.

---

### 🔬 TEST 1 — Health Check
```
Method:  GET
URL:     http://localhost:8080/actuator/health
```

**Expected (200 OK):**
```json
{ "status": "UP" }
```

---

### 🔬 TEST 2 — Register Through Gateway (public — no token needed)
```
Method:  POST
URL:     http://localhost:8080/api/users/auth/register
Headers: Content-Type: application/json
Body:
```
```json
{
    "fullName": "Vaishnavi Patil",
    "email": "gateway-test@carspa.com",
    "password": "password123"
}
```

**Expected (201 Created):**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "email": "gateway-test@carspa.com",
    "fullName": "Vaishnavi Patil",
    "role": "ROLE_USER"
}
```

📌 **Copy the `accessToken` — you need it for the next tests.**

---

### 🔬 TEST 3 — Login Through Gateway
```
Method:  POST
URL:     http://localhost:8080/api/users/auth/login
Headers: Content-Type: application/json
Body:
```
```json
{
    "email": "gateway-test@carspa.com",
    "password": "password123"
}
```

**Expected (200 OK):** Same token response as above.

---

### 🔬 TEST 4 — Access Protected Endpoint WITH Token ✅
```
Method:  GET
URL:     http://localhost:8080/api/users/profile
Headers: Authorization: Bearer <paste your token here>
```

**Expected (200 OK):**
```json
{
    "id": 1,
    "fullName": "Vaishnavi Patil",
    "email": "gateway-test@carspa.com",
    "role": "ROLE_USER",
    "active": true
}
```

This confirms:
- Gateway accepted the token ✅
- Gateway forwarded the request to user-service ✅
- user-service responded correctly ✅

---

### 🔬 TEST 5 — Access Protected Endpoint WITHOUT Token ❌
```
Method:  GET
URL:     http://localhost:8080/api/users/profile
Headers: (no Authorization header)
```

**Expected (401 Unauthorized):**
```
(empty body — gateway rejects before the request reaches user-service)
```

Check **Response Headers** in Postman — you'll see:
```
X-Auth-Error: Missing Authorization header
```

---

### 🔬 TEST 6 — Access Protected Endpoint With FAKE Token ❌
```
Method:  GET
URL:     http://localhost:8080/api/users/profile
Headers: Authorization: Bearer thisisafaketoken123
```

**Expected (401 Unauthorized)** — gateway rejects invalid signature.

---

### 🔬 TEST 7 — Try an Unregistered Service (503)
```
Method:  GET
URL:     http://localhost:8080/api/bookings
Headers: Authorization: Bearer <your valid token>
```

**Expected (503 Service Unavailable)** — booking-service isn't running yet.
This is correct behaviour — gateway is working, service just isn't up yet.

---

### 🔬 TEST 8 — Check Gateway Routes (actuator)
```
Method:  GET
URL:     http://localhost:8080/actuator/gateway/routes
```

**Expected (200 OK):** A JSON array listing all configured routes:
```json
[
  { "route_id": "user-service-public", ... },
  { "route_id": "user-service-protected", ... },
  { "route_id": "booking-service", ... },
  ...
]
```

---

## 📌 Service Summary

| Item                 | Value                                                   |
|----------------------|---------------------------------------------------------|
| Port                 | `8080`                                                  |
| Public paths         | `/api/users/auth/login`, `/api/users/auth/register`     |
| Protected paths      | Everything else — needs `Authorization: Bearer <token>` |
| Identity headers     | `X-User-Email`, `X-User-Id`, `X-User-Role`, `X-User-Name` |
| Routes view          | `GET http://localhost:8080/actuator/gateway/routes`     |
| Eureka required      | Yes — port 8761                                         |
| User service needed  | Yes — for routing `/api/users/**`                       |

---

## 📐 Routing Table

| URL prefix            | Routes to         | Auth required |
|-----------------------|-------------------|---------------|
| `/api/users/auth/**`  | user-service:8081 | ❌ No         |
| `/api/users/**`       | user-service:8081 | ✅ Yes        |
| `/api/cars/**`        | car-service:8083  | ✅ Yes        |
| `/api/bookings/**`    | booking-service:8082 | ✅ Yes     |
| `/api/payments/**`    | payment-service:8084 | ✅ Yes     |
| `/api/chat/**`        | chat-service:8086 | ✅ Yes        |

---

## ❌ Common Errors & Fixes

**`Could not resolve host: user-service`**
→ Eureka is not running. Start Service 1 first.

**`503 Service Unavailable` on /api/users/**`**
→ user-service isn't running or not yet registered in Eureka.
→ Wait 10–15 seconds after starting user-service for Eureka registration.

**`401` on every request even with a valid token**
→ JWT secret mismatch. Make sure `jwt.secret` in gateway
  matches `jwt.secret` in user-service `application.properties` exactly.

**`IllegalStateException: ... spring-boot-starter-web`**
→ You accidentally added `spring-boot-starter-web` to the gateway pom.
  Gateway uses WebFlux — they conflict. Remove it.

**`Port 8080 already in use`**
```cmd
netstat -ano | findstr :8080
taskkill /PID <number> /F
```

---

## ✅ Done!

Gateway is running. From here on, **all your Postman tests should use port 8080**.
Keep this window open and say **"next"** for **SERVICE 4 — Booking Service**.
