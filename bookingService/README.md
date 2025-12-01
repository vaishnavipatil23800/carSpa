# 🟠 SERVICE 4 — Booking Service

**Handles:** Create bookings · Slot conflict prevention · Cancel · Status updates · Admin stats

> ⚠️ **Prerequisites:**
> - Service 1 (Eureka) running on port 8761
> - Service 2 (User Service) running on port 8081
> - Service 3 (API Gateway) running on port 8080
> - MySQL running on port 3306
> - RabbitMQ running on port 5672 *(optional — bookings still work without it)*

---

## ✅ STEP 1 — Install RabbitMQ (optional but recommended)

RabbitMQ is used to send booking confirmation emails via notification-service.
If you skip this for now, bookings will still work — events just won't be published.

**Install RabbitMQ on Windows:**
1. Download Erlang: https://www.erlang.org/downloads  (required by RabbitMQ)
2. Download RabbitMQ: https://www.rabbitmq.com/install-windows.html
3. Run both installers
4. Open Command Prompt **as Administrator** and run:
```cmd
"C:\Program Files\RabbitMQ Server\rabbitmq_server-3.x.x\sbin\rabbitmq-service.bat" start
```

**Enable the Management UI:**
```cmd
"C:\Program Files\RabbitMQ Server\rabbitmq_server-3.x.x\sbin\rabbitmqctl.bat" enable rabbitmq_management
```

**Verify:** Open `http://localhost:15672` — login with `guest` / `guest`

---

## ✅ STEP 2 — Create MySQL Database

In MySQL Workbench run:
```sql
CREATE DATABASE IF NOT EXISTS bookingdb;
```

JPA will auto-create the `bookings` table when the service starts.

---

## ✅ STEP 3 — Open in IntelliJ

1. **File → Open** → select `04-booking-service`
2. Wait for Maven import

---

## ✅ STEP 4 — Run the Service

**IntelliJ:** Open `BookingServiceApplication.java` → click ▶️

**Command Prompt:**
```cmd
cd 04-booking-service
mvn spring-boot:run
```

**Wait for:**
```
Started BookingServiceApplication in X seconds
```

**Verify:** Open `http://localhost:8761` → you should see `BOOKING-SERVICE` registered.

---

## ✅ STEP 5 — Run Tests

```cmd
mvn test
```

7 unit tests — all run without MySQL, Eureka, or RabbitMQ.

---

## ✅ STEP 6 — Test With Postman

> 📌 **All requests go through the gateway on port 8080.**
> You need a valid JWT token from the login endpoint.
>
> **How to get a token:**
> ```
> POST http://localhost:8080/api/users/auth/login
> Body: { "email": "vaishnavi@test.com", "password": "password123" }
> ```
> Copy the `accessToken` from the response.

**For ALL requests below, add this header:**
```
Authorization: Bearer <your token>
```

---

### 🔬 TEST 1 — Create a Booking
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "vehicleNumber": "MH12AB1234",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "slotTime": "2025-12-25T10:00:00"
}
```

> ⚠️ The `slotTime` must be a **future** date/time.
> Change `2025-12-25` to any future date on your machine.

**Expected response (201 Created):**
```json
{
    "id": 1,
    "userId": 1,
    "userEmail": "vaishnavi@test.com",
    "vehicleNumber": "MH12AB1234",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "slotTime": "2025-12-25T10:00:00",
    "status": "CONFIRMED",
    "createdAt": "2024-..."
}
```

📌 **Save the booking `id` — you need it for the next tests.**

---

### 🔬 TEST 2 — Create SAME Slot (should fail with conflict)
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Headers: Authorization: Bearer <token>
Body:    (same as Test 1 — same washCentre, same slotTime)
```

**Expected response (409 Conflict):**
```json
{
    "title": "Booking Conflict",
    "detail": "Slot at Pune Central around 2025-12-25T10:00 is already booked. Please choose a different time.",
    "status": 409
}
```

This confirms the **slot conflict prevention** is working! ✅

---

### 🔬 TEST 3 — Create Booking at a Different Time (should succeed)
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Body:
```
```json
{
    "vehicleNumber": "MH12AB1234",
    "serviceType": "PREMIUM",
    "washCentre": "Pune Central",
    "slotTime": "2025-12-25T12:00:00"
}
```

> The slot is 2 hours away from the first booking — outside the 30-min window.
> **Expected: 201 Created** ✅

---

### 🔬 TEST 4 — Create Booking 15 Minutes Apart (should fail — within window)
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Body:
```
```json
{
    "vehicleNumber": "MH99ZZ9999",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "slotTime": "2025-12-25T10:15:00"
}
```

> Only 15 minutes from the first booking at 10:00 — within the 30-min window.
> **Expected: 409 Conflict** ✅

---

### 🔬 TEST 5 — Get My Bookings
```
Method:  GET
URL:     http://localhost:8080/api/bookings
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
[
    {
        "id": 1,
        "vehicleNumber": "MH12AB1234",
        "serviceType": "BASIC",
        "washCentre": "Pune Central",
        "slotTime": "2025-12-25T10:00:00",
        "status": "CONFIRMED"
    },
    {
        "id": 2,
        ...
        "status": "CONFIRMED"
    }
]
```

---

### 🔬 TEST 6 — Get Single Booking
```
Method:  GET
URL:     http://localhost:8080/api/bookings/1
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):** The booking object for ID 1.

---

### 🔬 TEST 7 — Cancel a Booking
```
Method:  PATCH
URL:     http://localhost:8080/api/bookings/1/cancel?reason=Change+of+plans
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
{
    "id": 1,
    "status": "CANCELLED",
    "cancellationReason": "Change of plans"
}
```

---

### 🔬 TEST 8 — Cancel Already Cancelled Booking (should fail)
```
Method:  PATCH
URL:     http://localhost:8080/api/bookings/1/cancel
Headers: Authorization: Bearer <token>
```

**Expected (409 Conflict):**
```json
{
    "title": "Booking Conflict",
    "detail": "Cannot cancel a booking with status: CANCELLED",
    "status": 409
}
```

---

### 🔬 TEST 9 — Admin: Update Booking Status
```
Method:  PATCH
URL:     http://localhost:8080/api/bookings/admin/2/status
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "status": "IN_PROGRESS"
}
```

**Expected (200 OK):** booking with `"status": "IN_PROGRESS"`

Then update to DONE:
```json
{ "status": "DONE" }
```

**Status must follow this order: CONFIRMED → IN_PROGRESS → DONE**
Skipping (e.g. CONFIRMED → DONE) returns **409 Conflict**.

---

### 🔬 TEST 10 — Admin: All Bookings
```
Method:  GET
URL:     http://localhost:8080/api/bookings/admin/all
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):** Array of all bookings in the system.

---

### 🔬 TEST 11 — Admin: Stats
```
Method:  GET
URL:     http://localhost:8080/api/bookings/admin/stats
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
{
    "total": 2,
    "pending": 0,
    "confirmed": 1,
    "inProgress": 0,
    "done": 1,
    "cancelled": 1,
    "byServiceType": {
        "BASIC": 1,
        "PREMIUM": 1
    }
}
```

---

### 🔬 TEST 12 — Invalid Vehicle Number Format
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Body:
```
```json
{
    "vehicleNumber": "invalid",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "slotTime": "2025-12-25T14:00:00"
}
```

**Expected (400 Bad Request):**
```json
{
    "title": "Validation Failed",
    "errors": {
        "vehicleNumber": "Vehicle number must be like MH12AB1234"
    }
}
```

---

### 🔬 TEST 13 — Past Slot Time (should fail)
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Body:
```
```json
{
    "vehicleNumber": "MH12AB1234",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "slotTime": "2020-01-01T10:00:00"
}
```

**Expected (400 Bad Request):**
```json
{
    "title": "Validation Failed",
    "errors": {
        "slotTime": "Slot time must be in the future"
    }
}
```

---

## 📌 Service Summary

| Item              | Value                                             |
|-------------------|---------------------------------------------------|
| Port              | `8082`                                            |
| Database          | `bookingdb` (MySQL)                               |
| Create booking    | `POST /api/bookings`                              |
| My bookings       | `GET  /api/bookings`                              |
| Cancel booking    | `PATCH /api/bookings/{id}/cancel`                 |
| Admin all         | `GET  /api/bookings/admin/all`                    |
| Admin stats       | `GET  /api/bookings/admin/stats`                  |
| Admin update      | `PATCH /api/bookings/admin/{id}/status`           |
| Slot window       | 30 minutes (configurable in application.properties)|
| RabbitMQ          | Optional — bookings work without it               |
| Swagger           | `http://localhost:8082/swagger-ui.html`           |

---

## ❌ Common Errors & Fixes

**`Connection refused: rabbitmq`**
→ RabbitMQ not running. Either install it, or ignore the warning — bookings still work.

**`Unknown database 'bookingdb'`**
→ Run `CREATE DATABASE bookingdb;` in MySQL Workbench.

**`401 Unauthorized` on every request**
→ Your token expired or you forgot the `Authorization: Bearer` header.

**`503 Service Unavailable`**
→ Booking service isn't registered in Eureka yet. Wait 15 seconds and retry.

---

## ✅ Done!

Keep this running and say **"next"** for **SERVICE 5 — Car Service**.
