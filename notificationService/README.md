# 🔔 SERVICE 7 — Notification Service

**Role:** Pure RabbitMQ consumer — no REST endpoints.  
Listens on booking and payment queues, sends styled HTML emails for every event.

> ⚠️ **Prerequisites:**
> - Service 1 (Eureka) running on 8761
> - RabbitMQ running on 5672
> - SMTP configured (Gmail App Password) — same as payment-service
> - Services 4 (booking) and 6 (payment) running to produce events

---

## 📐 How It Works

```
booking-service ──► carspa.booking.exchange ──► carspa.booking.queue ──► NotificationListener
                                                                                │
                                              ┌─────────────────────────────────┤
                                              │ booking.confirmed   → "Confirmed" email
                                              │ booking.cancelled   → "Cancelled" email
                                              │ booking.in_progress → "Wash started" email
                                              └ booking.done        → "Wash complete" email

payment-service ───► carspa.payment.exchange ──► carspa.payment.queue ──► NotificationListener
                                                                                │
                                              ┌─────────────────────────────────┤
                                              │ payment.success → "Payment confirmed" email
                                              └ payment.failed  → "Payment failed" email
```

---

## ✅ STEP 1 — Configure SMTP

Same Gmail App Password you used in payment-service.
Open `src/main/resources/application.properties`:

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
```

> If you skip this step, emails just won't send — service still starts fine.
> You'll see `WARN  Failed to send '...' email` in the console instead.

---

## ✅ STEP 2 — Verify RabbitMQ is Running

Open `http://localhost:15672` — login with `guest` / `guest`

You should see these queues already exist (created by booking/payment services):
- `carspa.booking.queue`
- `carspa.payment.queue`

If the queues don't exist yet, they'll be created when this service starts.

---

## ✅ STEP 3 — Open in IntelliJ

**File → Open** → `07-notification-service` → wait for Maven.

---

## ✅ STEP 4 — Run the Service

```cmd
cd 07-notification-service
mvn spring-boot:run
```

**Wait for:**
```
Started NotificationServiceApplication in X seconds
```

**Verify in console — you should see:**
```
[main] o.s.a.r.l.SimpleMessageListenerContainer : Listening on queue(s): carspa.booking.queue
[main] o.s.a.r.l.SimpleMessageListenerContainer : Listening on queue(s): carspa.payment.queue
```

---

## ✅ STEP 5 — Run Tests

```cmd
mvn test
```

10 unit tests — no RabbitMQ or SMTP needed. Tests verify the routing logic:
- Each eventType routes to exactly the right email method
- Unknown eventTypes are silently ignored
- Null events don't cause NullPointerException

---

## ✅ STEP 6 — Test End-to-End (trigger real emails)

The notification service has NO endpoints of its own.
You trigger it by performing actions on other services that publish events.

---

### 🔬 TEST 1 — Trigger `booking.confirmed` email

Create a new booking via the gateway:
```
Method:  POST
URL:     http://localhost:8080/api/bookings
Headers: Authorization: Bearer <token>
Body:    { "vehicleNumber": "MH12AB1234", "serviceType": "BASIC",
           "washCentre": "Pune Central", "slotTime": "2026-06-01T11:00:00" }
```

**What happens:**
1. booking-service saves booking with status CONFIRMED
2. booking-service publishes `booking.confirmed` event to RabbitMQ
3. notification-service receives the event
4. notification-service sends a "Booking Confirmed" HTML email

**In the notification-service console, look for:**
```
INFO  Booking event received: booking.confirmed for booking #X → your@email.com
INFO  Email sent → your@email.com [booking.confirmed]
```

**In your email inbox:** Blue branded email with booking details table.

---

### 🔬 TEST 2 — Trigger `booking.cancelled` email

Cancel the booking you just created:
```
Method:  PATCH
URL:     http://localhost:8080/api/bookings/{id}/cancel?reason=Testing
Headers: Authorization: Bearer <token>
```

**Expected in console:**
```
INFO  Booking event received: booking.cancelled for booking #X → your@email.com
INFO  Email sent → your@email.com [booking.cancelled]
```

---

### 🔬 TEST 3 — Trigger `booking.in_progress` and `booking.done` emails

Create a new booking (Test 1), then update its status as admin:

```
PATCH http://localhost:8080/api/bookings/admin/{id}/status
Body: { "status": "IN_PROGRESS" }
```
→ triggers `booking.in_progress` email ("Wash started 🧼")

Then:
```
PATCH http://localhost:8080/api/bookings/admin/{id}/status
Body: { "status": "DONE" }
```
→ triggers `booking.done` email ("Wash complete ✨")

---

### 🔬 TEST 4 — Trigger `payment.success` email

Complete the payment flow (create-order → verify) in payment-service.
After a successful `/api/payments/verify`:

**Expected in console:**
```
INFO  Payment event received: payment.success for payment #X → your@email.com
INFO  Email sent → your@email.com [payment.success]
```

**Note:** payment-service also sends an invoice email directly (separately from this service).
The notification-service sends a "payment confirmed" summary email.
Users will receive two emails — one with the PDF invoice, one with a summary.

---

### 🔬 TEST 5 — Verify via RabbitMQ Management UI

1. Go to `http://localhost:15672`
2. Click **Queues**
3. Select `carspa.booking.queue`

You should see:
- **Messages ready: 0** — all messages consumed immediately
- **Consumers: 1** — notification-service is listening
- **Message rates** — spikes when you create bookings

If **Messages ready > 0** and growing, the listener isn't consuming.
Check the console for errors.

---

### 🔬 TEST 6 — Test With SMTP Disabled (no crash expected)

Comment out your Gmail credentials in `application.properties`:
```properties
# spring.mail.username=...
# spring.mail.password=...
```

Restart the service, then create a booking.

**Expected in console:**
```
WARN  Failed to send 'booking.confirmed' email to user@test.com: ...
```

The service stays running, the RabbitMQ message is still acked.
No crash, no infinite retry loop. ✅

---

## 📌 Service Summary

| Item                  | Value                                                    |
|-----------------------|----------------------------------------------------------|
| Port                  | `8085`                                                   |
| Database              | **None** — stateless                                     |
| REST endpoints        | **None** — only RabbitMQ consumers                       |
| Booking queue         | `carspa.booking.queue`                                   |
| Payment queue         | `carspa.payment.queue`                                   |
| Booking emails        | confirmed, cancelled, in_progress, done                  |
| Payment emails        | success, failed                                          |
| Email failures        | Logged + skipped — never blocks queue processing         |
| Unknown event types   | Logged + skipped — no crash                              |

---

## 📧 Email Templates Summary

| Event                  | Subject                                          | Colour   |
|------------------------|--------------------------------------------------|----------|
| `booking.confirmed`    | Your CarSpa booking is confirmed! 🚗             | Green    |
| `booking.cancelled`    | Your CarSpa booking has been cancelled           | Red      |
| `booking.in_progress`  | Your car wash has started! 🧼                    | Orange   |
| `booking.done`         | Your car is sparkling clean! ✨                   | Blue     |
| `payment.success`      | Payment confirmed ₹X.XX — CarSpa Invoice         | Green    |
| `payment.failed`       | Payment failed — CarSpa Booking #X               | Red      |

---

## ❌ Common Errors & Fixes

**`Connection refused: rabbitmq:5672`**
→ RabbitMQ not running. Start it first (see Service 4 README for install steps).

**`No listener for queues found`**
→ Queue name in application.properties doesn't match what booking-service created.
→ Check `notification.booking-queue` matches exactly `carspa.booking.queue`.

**`ClassCastException` or deserialization error in listener**
→ The event POJO fields don't match what was published.
→ Check `BookingEvent.java` here matches `BookingEvent.java` in booking-service.

**`javax.mail.AuthenticationFailedException`**
→ Gmail App Password wrong or expired. Regenerate at myaccount.google.com/apppasswords.

**Emails not arriving**
→ Check spam folder. Gmail can flag programmatic emails.
→ Check console for "Email sent" INFO log — if present, the issue is on Gmail's end.

**`Channel shutdown: channel error... PRECONDITION_FAILED`**
→ Exchange/queue declared with different settings (e.g. durable vs non-durable).
→ Delete the queue from RabbitMQ UI (Queues → Delete) and restart all services.

---

## ✅ Done!

You now have a fully event-driven notification pipeline.
When bookings change state or payments complete, emails go out automatically.

Say **"next"** for **SERVICE 8 — Chat Service** (OpenAI GPT-powered assistant).
