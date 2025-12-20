# 💳 SERVICE 6 — Payment Service

**Handles:** Razorpay order creation · HMAC-SHA256 verification · iText 7 PDF invoices · Email delivery · Revenue reporting

> ⚠️ **Prerequisites:**
> - Services 1–5 all running
> - MySQL on 3306
> - RabbitMQ on 5672 *(optional — payments still succeed without it)*
> - Gmail App Password *(optional — email is skipped if not configured)*

---

## ✅ STEP 1 — Get Razorpay Test Credentials

1. Go to **https://dashboard.razorpay.com**
2. Sign up for a free account (no real money — test mode)
3. From the dashboard: **Settings → API Keys → Generate Test Key**
4. Copy your **Key ID** and **Key Secret**

> 💡 In test mode you can use fake card `4111 1111 1111 1111` (expiry: any future, CVV: any 3 digits)

---

## ✅ STEP 2 — Configure Razorpay Credentials

Open `src/main/resources/application.properties` and replace:

```properties
razorpay.key-id=rzp_test_REPLACE_WITH_YOUR_KEY
razorpay.key-secret=REPLACE_WITH_YOUR_SECRET
```

with your actual test credentials.

---

## ✅ STEP 3 — Configure Gmail SMTP (optional — skip if email not needed)

1. Go to **myaccount.google.com → Security → 2-Step Verification → App Passwords**
2. Generate a new App Password for "Mail"
3. In `application.properties`:
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
```

If you skip this, payments still work — the invoice PDF is generated locally but not emailed.

---

## ✅ STEP 4 — Create MySQL Database

```sql
CREATE DATABASE IF NOT EXISTS paymentdb;
```

---

## ✅ STEP 5 — Open in IntelliJ

**File → Open** → `06-payment-service` → wait for Maven.

---

## ✅ STEP 6 — Run the Service

```cmd
cd 06-payment-service
mvn spring-boot:run
```

**Wait for:** `Started PaymentServiceApplication in X seconds`

Verify in Eureka: `http://localhost:8761` → `PAYMENT-SERVICE` appears.

---

## ✅ STEP 7 — Run Tests

```cmd
mvn test
```

6 unit tests — no DB, Razorpay, or SMTP needed.

---

## ✅ STEP 8 — Test With Postman

> 📌 All requests via **port 8080 (gateway)**.
> Get your token: `POST http://localhost:8080/api/users/auth/login`

---

### 🔬 TEST 1 — Create Razorpay Order (Step 1 of payment)
```
Method:  POST
URL:     http://localhost:8080/api/payments/create-order
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "bookingId": 1,
    "vehicleNumber": "MH12AB1234",
    "serviceType": "BASIC",
    "washCentre": "Pune Central",
    "amount": 299.00
}
```

**Expected (201 Created):**
```json
{
    "paymentId": 1,
    "razorpayOrderId": "order_AbCdEfGhIjKlMn",
    "currency": "INR",
    "baseAmount": 299.00,
    "gstAmount": 53.82,
    "totalAmount": 352.82,
    "amountInPaise": 35282,
    "status": "CREATED"
}
```

📌 **Save the `razorpayOrderId`** — needed for the verify step.

**GST breakdown:**
- Base: ₹299.00
- GST (18%): ₹53.82
- **Total: ₹352.82**

---

### 🔬 TEST 2 — Check Payment Status (before verification)
```
Method:  GET
URL:     http://localhost:8080/api/payments/my
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):** Array with your payment at status `"CREATED"`.

---

### 🔬 TEST 3 — Simulate Payment Verification

> ⚠️ **In real usage**, the frontend handles step 2 (user pays in Razorpay modal).
> Razorpay provides `razorpayPaymentId` and `razorpaySignature` to the frontend.
>
> **For Postman testing**, you need to generate a valid HMAC signature.
> Here's how to do it in Postman:

**In Postman → Pre-request Script tab:**
```javascript
const orderId    = "order_AbCdEfGhIjKlMn";  // from Test 1
const paymentId  = "pay_TestPaymentId";      // simulate
const secret     = "REPLACE_WITH_YOUR_SECRET";
const data       = orderId + "|" + paymentId;

const sig = CryptoJS.HmacSHA256(data, secret).toString(CryptoJS.enc.Hex);
pm.environment.set("razorpay_sig", sig);
```

Then set your body to:
```json
{
    "razorpayOrderId": "order_AbCdEfGhIjKlMn",
    "razorpayPaymentId": "pay_TestPaymentId",
    "razorpaySignature": "{{razorpay_sig}}"
}
```

**Method:**  POST
**URL:**     `http://localhost:8080/api/payments/verify`

**Expected (200 OK):**
```json
{
    "id": 1,
    "status": "SUCCESS",
    "razorpayPaymentId": "pay_TestPaymentId",
    "totalAmount": 352.82
}
```

After this:
- PDF invoice generated in `/tmp/carspa-invoices/INV-00001.pdf`
- Invoice email sent to your email (if SMTP configured)
- Payment event published to RabbitMQ

---

### 🔬 TEST 4 — Download Invoice PDF
```
Method:  GET
URL:     http://localhost:8080/api/payments/1/invoice
Headers: Authorization: Bearer <token>
```

**Expected:** A PDF file downloads automatically.

In Postman:
- Click **Send**
- Click **Save Response → Save to file**
- Open the downloaded `.pdf`
- You should see the CarSpa branded invoice with GST breakdown

---

### 🔬 TEST 5 — Try Invoice on FAILED/CREATED Payment
```
Method:  GET
URL:     http://localhost:8080/api/payments/1/invoice
(if payment is not SUCCESS)
```

**Expected (409 Conflict):**
```json
{
    "title": "Invalid Request",
    "detail": "Invoice only available for successful payments"
}
```

---

### 🔬 TEST 6 — Admin: Revenue Stats
```
Method:  GET
URL:     http://localhost:8080/api/payments/admin/revenue
Headers: Authorization: Bearer <token>
```

**Expected (200 OK):**
```json
{
    "totalRevenue": 352.82,
    "successCount": 1,
    "failedCount": 0,
    "revenueByServiceType": {
        "BASIC": 352.82
    }
}
```

Create more payments (PREMIUM and FULL_DETAIL) and run again to see it break down by service type.

---

### 🔬 TEST 7 — Wrong user accessing invoice
```
Method:  GET
URL:     http://localhost:8080/api/payments/1/invoice
Headers: Authorization: Bearer <token-of-different-user>
```

**Expected (403 Forbidden)** — access denied.

---

### 🔬 TEST 8 — Missing required fields
```
Method:  POST
URL:     http://localhost:8080/api/payments/create-order
Body:
```
```json
{
    "bookingId": 1,
    "amount": -100
}
```

**Expected (400 Bad Request):**
```json
{
    "title": "Validation Failed",
    "errors": {
        "vehicleNumber": "Vehicle number is required",
        "serviceType": "Service type is required",
        "washCentre": "Wash centre is required",
        "amount": "Amount must be positive"
    }
}
```

---

## 📌 Service Summary

| Item                | Value                                                    |
|---------------------|----------------------------------------------------------|
| Port                | `8084`                                                   |
| Database            | `paymentdb` (MySQL)                                      |
| Razorpay mode       | Test mode — no real money charged                        |
| Create order        | `POST /api/payments/create-order`                        |
| Verify payment      | `POST /api/payments/verify`                              |
| My payments         | `GET  /api/payments/my`                                  |
| Download invoice    | `GET  /api/payments/{id}/invoice`                        |
| Admin revenue       | `GET  /api/payments/admin/revenue`                       |
| GST rate            | 18% (configurable via `payment.gst-rate`)                |
| Invoice location    | `/tmp/carspa-invoices/INV-xxxxx.pdf`                     |
| Email               | Async — payment succeeds even if email fails             |
| Swagger             | `http://localhost:8084/swagger-ui.html`                  |

---

## 📐 Razorpay Integration Diagram

```
Frontend                  Payment Service           Razorpay
    │                          │                       │
    │── POST /create-order ───►│── createOrder() ─────►│
    │                          │                       │
    │◄── { orderId, amount } ──│◄─── { order_xxx } ────│
    │                          │                       │
    │────── open modal ────────────────────────────────►│
    │                                                   │
    │◄── { paymentId, signature } ──────────────────────│
    │                          │                       │
    │── POST /verify ─────────►│                       │
    │                          │── HMAC check ─────────│
    │                          │── SUCCESS → invoice   │
    │◄── { status: SUCCESS } ──│                       │
```

---

## ❌ Common Errors & Fixes

**`RazorpayException: Authentication failed`**
→ Wrong `razorpay.key-id` or `razorpay.key-secret` in application.properties.

**`Unknown database 'paymentdb'`**
→ Run `CREATE DATABASE paymentdb;` in MySQL Workbench.

**`javax.mail.AuthenticationFailedException`**
→ Gmail App Password is wrong or 2FA is not enabled.
→ You can comment out all `spring.mail.*` lines to disable email entirely.

**PDF invoice is blank or won't open**
→ iText dependency issue — check Maven downloaded `kernel` and `layout` jars.
→ Run `mvn dependency:resolve` to verify.

**`402 Payment Required` on /verify**
→ HMAC signature mismatch. The signature in the request doesn't match what we computed.
→ Double check the Postman Pre-request Script uses the correct `key-secret`.

---

## ✅ Done!

PDF invoice working and revenue stats returning data.

Say **"next"** for **SERVICE 7 — Notification Service** (RabbitMQ email consumers).
