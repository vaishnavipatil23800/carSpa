# 🟢 SERVICE 1 — Eureka Service (Service Registry)

## What This Service Does
Eureka is the **service registry** for the entire CarSpa system.
Every other microservice (user-service, booking-service, etc.) registers
itself here when it starts. This is how they find and call each other.

> ⚠️ **You MUST start this service FIRST before starting any other service.**

---

## ✅ STEP 1 — Check Your Setup

Open **Command Prompt** and run these one by one:

```cmd
java -version
```
You should see: `java version "21"` or higher.
If not → download Java 21 from https://adoptium.net

```cmd
mvn -version
```
You should see: `Apache Maven 3.x.x`
If not → download Maven from https://maven.apache.org/download.cgi

---

## ✅ STEP 2 — Open the Project in IntelliJ

1. Open **IntelliJ IDEA**
2. Click **File → Open**
3. Navigate to the `01-eureka-service` folder
4. Click **OK**
5. Wait for IntelliJ to import the Maven project (bottom progress bar)

---

## ✅ STEP 3 — Run the Service

**Option A — Run from IntelliJ:**
1. Open `src/main/java/com/carspa/eureka/EurekaServiceApplication.java`
2. Click the green ▶️ button next to `public static void main`
3. Watch the console — wait until you see:
   ```
   Started EurekaServiceApplication in X seconds
   ```

**Option B — Run from Command Prompt:**
```cmd
cd 01-eureka-service
mvn spring-boot:run
```

---

## ✅ STEP 4 — Verify It's Working

Open your browser and go to:
```
http://localhost:8761
```

You should see the **Eureka Dashboard** — a green page that says
"Instances currently registered with Eureka"

Right now the list will be **empty** — that's normal!
Services will appear here as you start them one by one.

---

## ✅ STEP 5 — Test With Postman (Health Check)

1. Open **Postman**
2. Click **New Request**
3. Set method to **GET**
4. Enter URL:
   ```
   http://localhost:8761/actuator/health
   ```
5. Click **Send**

✅ You should get:
```json
{
    "status": "UP"
}
```

---

## 📌 Important Notes

| Item | Value |
|------|-------|
| Port | `8761` |
| Dashboard URL | `http://localhost:8761` |
| Health check | `http://localhost:8761/actuator/health` |
| Start order | **FIRST — before everything else** |
| Stop it? | Only after you stop ALL other services first |

---

## ❌ Common Errors & Fixes

**Error: `Port 8761 already in use`**
```cmd
netstat -ano | findstr :8761
```
Find the PID number in the last column, then:
```cmd
taskkill /PID <that-number> /F
```

**Error: `java.lang.UnsupportedClassVersionError`**
Your Java version is too old. Install Java 21 from https://adoptium.net

**Error: Maven dependencies not downloading**
Run this:
```cmd
mvn dependency:resolve
```

---

## ✅ Done!

Eureka is running. Now move to **SERVICE 2 — User Service**.
Keep this window open — don't close it!
