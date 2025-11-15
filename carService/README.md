# 🚗 SERVICE 5 — Car Service

**Manages:** User vehicles · Wash centre catalogue · Caching · Admin centre management

> ⚠️ **Prerequisites:**
> - Service 1 (Eureka) running on 8761
> - Service 3 (API Gateway) running on 8080
> - MySQL running on 3306

---

## ✅ STEP 1 — Create MySQL Database

In MySQL Workbench:
```sql
CREATE DATABASE IF NOT EXISTS cardb;
```

JPA auto-creates the `vehicles` and `wash_centres` tables on startup.
`data.sql` seeds 7 wash centres in Pune/Mumbai/Bangalore automatically — the frontend won't be empty.

---

## ✅ STEP 2 — Open in IntelliJ

**File → Open** → select `05-car-service` → wait for Maven import.

---

## ✅ STEP 3 — Run the Service

**IntelliJ:** Open `CarServiceApplication.java` → click ▶️

**Command Prompt:**
```cmd
cd 05-car-service
mvn spring-boot:run
```

**Wait for:**
```
Started CarServiceApplication in X seconds
```

**Verify:** Open `http://localhost:8761` — you should now see `CAR-SERVICE` registered.

---

## ✅ STEP 4 — Run Tests

```cmd
mvn test
```

8 unit tests — no DB or Eureka needed.

---

## ✅ STEP 5 — Test With Postman

> 📌 All requests go through **port 8080 (gateway)**.
> Get your token first:
> ```
> POST http://localhost:8080/api/users/auth/login
> Body: { "email": "your@email.com", "password": "yourpassword" }
> ```

**Add to all protected requests:**
```
Authorization: Bearer <your token>
```

---

### 🔬 TEST 1 — List All Wash Centres (first request hits DB, then cached)
```
Method:  GET
URL:     http://localhost:8080/api/cars/centres
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
[
    {
        "id": 1,
        "name": "Pune Central",
        "address": "MG Road, Camp, Pune",
        "city": "Pune",
        "pincode": "411001",
        "operatingHours": "07:00-21:00",
        "priceBasic": 299.00,
        "pricePremium": 499.00,
        "priceFullDetail": 899.00,
        "capacity": 4,
        "active": true
    },
    ...7 centres total
]
```

📌 **Copy a centre name** (e.g. `"Pune Central"`) — you'll use it when testing booking-service.

---

### 🔬 TEST 2 — Filter Centres by City
```
Method:  GET
URL:     http://localhost:8080/api/cars/centres/city/Pune
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):** Array of 5 Pune centres only.

Try with `Mumbai` and `Bangalore` too.

---

### 🔬 TEST 3 — Get Single Centre by ID
```
Method:  GET
URL:     http://localhost:8080/api/cars/centres/1
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):** The "Pune Central" centre details.

---

### 🔬 TEST 4 — Add a Vehicle
```
Method:  POST
URL:     http://localhost:8080/api/cars/vehicles
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "vehicleNumber": "MH12AB1234",
    "vehicleType": "SEDAN",
    "brand": "Maruti",
    "model": "Swift",
    "color": "White"
}
```

**Expected (201 Created):**
```json
{
    "id": 1,
    "userId": 1,
    "vehicleNumber": "MH12AB1234",
    "vehicleType": "SEDAN",
    "brand": "Maruti",
    "model": "Swift",
    "color": "White",
    "active": true
}
```

---

### 🔬 TEST 5 — Add Same Vehicle Again (duplicate — should fail)
```
Same request as Test 4
```

**Expected (409 Conflict):**
```json
{
    "title": "Conflict",
    "detail": "Vehicle MH12AB1234 is already registered to your account"
}
```

---

### 🔬 TEST 6 — Add a Second Vehicle
```
Method:  POST
URL:     http://localhost:8080/api/cars/vehicles
Body:
```
```json
{
    "vehicleNumber": "MH14XY5678",
    "vehicleType": "SUV",
    "brand": "Hyundai",
    "model": "Creta",
    "color": "Black"
}
```

**Expected (201 Created)** ✅

---

### 🔬 TEST 7 — List My Vehicles
```
Method:  GET
URL:     http://localhost:8080/api/cars/vehicles
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
[
    { "id": 1, "vehicleNumber": "MH12AB1234", "vehicleType": "SEDAN", ... },
    { "id": 2, "vehicleNumber": "MH14XY5678", "vehicleType": "SUV", ... }
]
```

---

### 🔬 TEST 8 — Update a Vehicle
```
Method:  PUT
URL:     http://localhost:8080/api/cars/vehicles/1
Headers: Authorization: Bearer <token>
Body:
```
```json
{
    "vehicleNumber": "MH12AB1234",
    "vehicleType": "SEDAN",
    "brand": "Maruti",
    "model": "Swift Dzire",
    "color": "Silver"
}
```

**Expected (200 OK):** Vehicle with updated model and color.

---

### 🔬 TEST 9 — Delete a Vehicle
```
Method:  DELETE
URL:     http://localhost:8080/api/cars/vehicles/1
Headers: Authorization: Bearer <token>
```

**Expected (204 No Content)** — vehicle soft-deleted (active = false, not removed from DB).

Then call **Test 7** again — vehicle 1 should no longer appear in the list.

---

### 🔬 TEST 10 — Invalid Vehicle Number Format
```
Method:  POST
URL:     http://localhost:8080/api/cars/vehicles
Body:
```
```json
{
    "vehicleNumber": "abc123",
    "vehicleType": "SEDAN"
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

### 🔬 TEST 11 — Admin: Create a New Wash Centre
```
Method:  POST
URL:     http://localhost:8080/api/cars/admin/centres
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "name": "Baner QuickWash",
    "address": "Baner Road, Baner, Pune",
    "city": "Pune",
    "pincode": "411045",
    "phone": "9876501234",
    "operatingHours": "08:00-21:00",
    "priceBasic": 279.00,
    "pricePremium": 479.00,
    "priceFullDetail": 879.00,
    "capacity": 3
}
```

**Expected (201 Created)** ✅ — new centre appears in Test 1 next time.

---

### 🔬 TEST 12 — Admin: Deactivate a Centre
```
Method:  DELETE
URL:     http://localhost:8080/api/cars/admin/centres/7
Headers: Authorization: Bearer <token>
```

**Expected (204 No Content)** — centre deactivated (soft delete), won't appear in Test 1.

---

## 📌 Service Summary

| Item                | Value                                              |
|---------------------|----------------------------------------------------|
| Port                | `8083`                                             |
| Database            | `cardb` (MySQL)                                    |
| Seed data           | 7 wash centres inserted automatically via data.sql |
| List centres        | `GET  /api/cars/centres`                           |
| Filter by city      | `GET  /api/cars/centres/city/{city}`               |
| My vehicles         | `GET  /api/cars/vehicles`                          |
| Add vehicle         | `POST /api/cars/vehicles`                          |
| Admin: new centre   | `POST /api/cars/admin/centres`                     |
| Caching             | Caffeine — wash centres cached 60s, auto-evicted on write |
| Swagger             | `http://localhost:8083/swagger-ui.html`            |

---

## ❌ Common Errors & Fixes

**`Unknown database 'cardb'`**
→ Run `CREATE DATABASE cardb;` in MySQL Workbench.

**Wash centres list is empty**
→ Make sure `spring.jpa.defer-datasource-initialization=true` is NOT set — `data.sql` needs JPA to run first.
→ Or manually run the INSERT statements in `data.sql` in MySQL Workbench.

**`503` calling `/api/cars/**`**
→ Car service not yet registered in Eureka. Wait 15 seconds and try again.

---

## ✅ Done!

All 5 services running — Eureka, User, Gateway, Booking, Car.

Say **"next"** for **SERVICE 6 — Payment Service** (Razorpay + PDF invoices).
