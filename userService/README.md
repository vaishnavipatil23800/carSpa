# 🔵 SERVICE 2 — User Service

**Handles:** Registration · Login · JWT issuance · Profile · Admin user management

> ⚠️ **Prerequisites:** Service 1 (Eureka) must already be running on port 8761.

---

## ✅ STEP 1 — Set Up MySQL Database

You need MySQL 8.0 running locally.

**If you don't have MySQL:**
Download from → https://dev.mysql.com/downloads/installer/
During install, set root password to `root` (matches application.properties).

**Create the database:**

1. Open **MySQL Workbench** (or MySQL command line)
2. Run this SQL:

```sql
CREATE DATABASE IF NOT EXISTS userdb;
```

3. Click the lightning bolt ⚡ button to execute, or press `Ctrl+Enter`

That's it — JPA will auto-create the tables when the service starts.

---

## ✅ STEP 2 — Open in IntelliJ

1. Open **IntelliJ IDEA**
2. **File → Open** → select the `02-user-service` folder
3. Wait for Maven import to finish (watch the bottom progress bar)
4. If prompted "Maven project found" → click **Load Maven Project**

---

## ✅ STEP 3 — Check application.properties

Open `src/main/resources/application.properties`

Make sure these match your MySQL setup:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/userdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

If your MySQL password is different, update it here.

---

## ✅ STEP 4 — Run the Service

**Option A — IntelliJ:**
1. Open `UserServiceApplication.java`
2. Click the green ▶️ button

**Option B — Command Prompt:**
```cmd
cd 02-user-service
mvn spring-boot:run
```

**Wait for this in the console:**
```
Started UserServiceApplication in X seconds
```

**Verify registration with Eureka:**
→ Open `http://localhost:8761`
→ You should see `USER-SERVICE` appear in the instances list

---

## ✅ STEP 5 — Run the Tests

```cmd
mvn test
```

You should see `5 tests passed` — these run without needing MySQL or Eureka.

---

## ✅ STEP 6 — Test With Postman

Open **Postman** and follow each test in order.

---

### 🔬 TEST 1 — Health Check
```
Method:  GET
URL:     http://localhost:8081/actuator/health
Headers: (none)
```

**Expected response (200 OK):**
```json
{
    "status": "UP"
}
```

---

### 🔬 TEST 2 — Register a New User
```
Method:  POST
URL:     http://localhost:8081/api/users/auth/register
Headers: Content-Type: application/json
Body (raw JSON):
```
```json
{
    "fullName": "Vaishnavi Patil",
    "email": "vaishnavi@test.com",
    "password": "password123",
    "phone": "9876543210"
}
```

**Expected response (201 Created):**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "email": "vaishnavi@test.com",
    "fullName": "Vaishnavi Patil",
    "role": "ROLE_USER"
}
```

📌 **Copy the `accessToken` value — you will need it for all tests below.**

---

### 🔬 TEST 3 — Register Duplicate Email (should fail)
```
Method:  POST
URL:     http://localhost:8081/api/users/auth/register
Body:
```
```json
{
    "fullName": "Another Person",
    "email": "vaishnavi@test.com",
    "password": "password123"
}
```

**Expected response (409 Conflict):**
```json
{
    "title": "Registration Failed",
    "detail": "Email already registered: vaishnavi@test.com",
    "status": 409
}
```

---

### 🔬 TEST 4 — Login
```
Method:  POST
URL:     http://localhost:8081/api/users/auth/login
Headers: Content-Type: application/json
Body:
```
```json
{
    "email": "vaishnavi@test.com",
    "password": "password123"
}
```

**Expected response (200 OK):**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "email": "vaishnavi@test.com",
    "fullName": "Vaishnavi Patil",
    "role": "ROLE_USER"
}
```

---

### 🔬 TEST 5 — Login With Wrong Password (should fail)
```
Method:  POST
URL:     http://localhost:8081/api/users/auth/login
Body:
```
```json
{
    "email": "vaishnavi@test.com",
    "password": "wrongpassword"
}
```

**Expected response (401 Unauthorized):**
```json
{
    "title": "Invalid Credentials",
    "detail": "Email or password is incorrect",
    "status": 401
}
```

---

### 🔬 TEST 6 — Get Profile (needs token)
```
Method:  GET
URL:     http://localhost:8081/api/users/profile
Headers: Authorization: Bearer <paste your token here>
```

To add the header in Postman:
1. Go to the **Headers** tab
2. Add key: `Authorization`
3. Add value: `Bearer eyJhbGciOiJIUzI1NiJ9...` (your actual token)

**Expected response (200 OK):**
```json
{
    "id": 1,
    "fullName": "Vaishnavi Patil",
    "email": "vaishnavi@test.com",
    "phone": "9876543210",
    "role": "ROLE_USER",
    "active": true
}
```

---

### 🔬 TEST 7 — Get Profile Without Token (should fail)
```
Method:  GET
URL:     http://localhost:8081/api/users/profile
Headers: (no Authorization header)
```

**Expected response (403 Forbidden)** — Spring Security blocks it.

---

### 🔬 TEST 8 — Register an Admin User
```
Method:  POST
URL:     http://localhost:8081/api/users/auth/register
Body:
```
```json
{
    "fullName": "Admin User",
    "email": "admin@carspa.com",
    "password": "adminpass123"
}
```

After registering, you need to manually update the role in MySQL:
```sql
USE userdb;
-- Find the admin user's ID first
SELECT id FROM users WHERE email = 'admin@carspa.com';

-- Then insert the ADMIN role (replace 2 with the actual ID you see)
INSERT INTO user_roles (user_id, role) VALUES (2, 'ROLE_ADMIN');

-- Remove the default USER role
DELETE FROM user_roles WHERE user_id = 2 AND role = 'ROLE_USER';
```

Then login again as admin to get an admin JWT token.

---

### 🔬 TEST 9 — Admin: List All Users
```
Method:  GET
URL:     http://localhost:8081/api/users/admin/all
Headers: Authorization: Bearer <ADMIN token>
```

**Expected response (200 OK):**
```json
[
    {
        "id": 1,
        "fullName": "Vaishnavi Patil",
        "email": "vaishnavi@test.com",
        "role": "ROLE_USER",
        "active": true
    },
    {
        "id": 2,
        "fullName": "Admin User",
        "email": "admin@carspa.com",
        "role": "ROLE_ADMIN",
        "active": true
    }
]
```

---

### 🔬 TEST 10 — Swagger UI
Open your browser:
```
http://localhost:8081/swagger-ui.html
```

You'll see all endpoints listed. Click **Authorize** (top right), paste your token, and test from the browser directly.

---

## 📌 Service Summary

| Item             | Value                                      |
|------------------|--------------------------------------------|
| Port             | `8081`                                     |
| Register         | `POST /api/users/auth/register`            |
| Login            | `POST /api/users/auth/login`               |
| Profile          | `GET  /api/users/profile` *(needs token)*  |
| All users        | `GET  /api/users/admin/all` *(admin only)* |
| Swagger UI       | `http://localhost:8081/swagger-ui.html`    |
| Eureka required  | Yes — must be on port 8761                 |
| MySQL DB         | `userdb`                                   |

---

## ❌ Common Errors & Fixes

**`Access denied for user 'root'@'localhost'`**
→ Wrong MySQL password in `application.properties`. Update `spring.datasource.password`

**`Unknown database 'userdb'`**
→ You forgot to create the DB. Run: `CREATE DATABASE userdb;` in MySQL Workbench

**`Connection refused 8761`**
→ Eureka service is not running. Start Service 1 first.

**`IllegalArgumentException: The specified key byte array is X bits...`**
→ Your `jwt.secret` is too short — must be at least 32 characters.

**`Port 8081 already in use`**
```cmd
netstat -ano | findstr :8081
taskkill /PID <number> /F
```

---

## ✅ Done!

User Service is running. Keep this window open and say **"next"** for **SERVICE 3 — API Gateway**.
